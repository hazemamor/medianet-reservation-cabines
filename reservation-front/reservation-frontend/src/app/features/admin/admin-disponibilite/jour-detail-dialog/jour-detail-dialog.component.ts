import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AdminReservationService } from '../../../../core/services/admin-reservation.service';
import { ReservationAdminResponse, UtilisateurResume } from '../../../../core/models/reservation-admin.model';

export interface JourDetailDialogData {
  cabineId: number;
  cabineNom: string;
  cabineNumero: string;
  date: Date;
  heureDisponibiliteDebut: string;
  heureDisponibiliteFin: string;
  reservations: ReservationAdminResponse[];
}

@Component({
  selector: 'app-jour-detail-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatFormFieldModule, MatSelectModule, MatInputModule, MatSnackBarModule
  ],
  templateUrl: './jour-detail-dialog.component.html'
})
export class JourDetailDialogComponent implements OnInit {

  private adminReservationService = inject(AdminReservationService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  actionEnCours = signal<number | null>(null);
  /** true si une annulation ou une nouvelle reservation a eu lieu : le parent doit recharger a la fermeture. */
  uneModificationAEteFaite = false;

  employes = signal<UtilisateurResume[]>([]);
  chargementEmployes = signal(true);
  creationEnCours = signal(false);
  afficherFormulaire = signal(false);

  /** Une reservation ne peut etre creee que pour aujourd'hui ou une date future. */
  jourPasse = false;

  formNouvelleReservation = this.fb.group({
    utilisateurId: [null as number | null, Validators.required],
    heureDebut: ['', Validators.required],
    heureFin: ['', Validators.required]
  });

  /**
   * true si la duree totale des reservations non annulees couvre (au moins) toute la plage
   * de disponibilite de la cabine ce jour-la : dans ce cas, l'admin ne peut pas reserver un
   * nouveau creneau tant qu'il n'a pas annule une reservation existante pour en liberer un.
   * Recalcule a chaque appel (et non fige a l'ouverture), car data.reservations est mis a jour
   * en place par annuler() : une annulation dans la session rouvre donc immediatement le formulaire.
   */
  jourDejaComplet(): boolean {
    const [hD, mD] = this.data.heureDisponibiliteDebut.substring(0, 5).split(':').map(Number);
    const [hF, mF] = this.data.heureDisponibiliteFin.substring(0, 5).split(':').map(Number);
    const dureeTotale = Math.max(0, (hF * 60 + mF) - (hD * 60 + mD));
    if (dureeTotale === 0) {
      return false;
    }
    const dureeReservee = this.data.reservations
      .filter(r => r.statut !== 'ANNULEE')
      .reduce((total, r) => {
        const debut = new Date(r.dateDebut).getTime();
        const fin = new Date(r.dateFin).getTime();
        return total + Math.max(0, (fin - debut) / 60000);
      }, 0);
    return dureeReservee >= dureeTotale;
  }

  constructor(
    public dialogRef: MatDialogRef<JourDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: JourDetailDialogData
  ) {}

  ngOnInit(): void {
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);
    const jourCellule = new Date(this.data.date);
    jourCellule.setHours(0, 0, 0, 0);
    this.jourPasse = jourCellule < aujourdhui;

    this.formNouvelleReservation.patchValue({
      heureDebut: this.data.heureDisponibiliteDebut.substring(0, 5),
      heureFin: this.data.heureDisponibiliteFin.substring(0, 5)
    });

    this.adminReservationService.listerEmployes().subscribe({
      next: (employes) => { this.employes.set(employes); this.chargementEmployes.set(false); },
      error: () => this.chargementEmployes.set(false)
    });
  }

  /** Seule une reservation future (pas encore commencee) et non deja annulee peut etre annulee. */
  peutAnnuler(reservation: ReservationAdminResponse): boolean {
    return reservation.statut !== 'ANNULEE' && new Date(reservation.dateDebut) > new Date();
  }

  annuler(reservation: ReservationAdminResponse): void {
    if (!confirm(`Annuler la réservation de ${reservation.employeEmail} ?`)) {
      return;
    }
    this.actionEnCours.set(reservation.id);
    this.adminReservationService.annuler(reservation.id).subscribe({
      next: (miseAJour) => {
        this.actionEnCours.set(null);
        const index = this.data.reservations.findIndex(r => r.id === reservation.id);
        if (index !== -1) {
          this.data.reservations[index] = { ...this.data.reservations[index], statut: miseAJour.statut };
        }
        this.uneModificationAEteFaite = true;
        this.snackBar.open('Réservation annulée', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.actionEnCours.set(null);
        this.snackBar.open(err.error?.message ?? 'Erreur lors de l\'annulation', 'Fermer', { duration: 5000 });
      }
    });
  }

  /** Formate une Date locale en "yyyy-MM-dd" sans passer par UTC (evite les decalages de jour). */
  private formatDateLocale(date: Date): string {
    const annee = date.getFullYear();
    const mois = String(date.getMonth() + 1).padStart(2, '0');
    const jour = String(date.getDate()).padStart(2, '0');
    return `${annee}-${mois}-${jour}`;
  }

  reserverPourEmploye(): void {
    if (this.formNouvelleReservation.invalid) {
      this.formNouvelleReservation.markAllAsTouched();
      return;
    }

    const { utilisateurId, heureDebut, heureFin } = this.formNouvelleReservation.value;
    const jour = this.formatDateLocale(this.data.date);
    const dateDebut = `${jour}T${heureDebut}:00`;
    const dateFin = `${jour}T${heureFin}:00`;

    this.creationEnCours.set(true);
    this.adminReservationService.reserverPourEmploye({
      utilisateurId: utilisateurId!,
      cabineId: this.data.cabineId,
      dateDebut,
      dateFin
    }).subscribe({
      next: (nouvelleReservation) => {
        this.creationEnCours.set(false);
        this.data.reservations.push(nouvelleReservation);
        this.uneModificationAEteFaite = true;
        this.afficherFormulaire.set(false);
        this.formNouvelleReservation.patchValue({ utilisateurId: null });
        this.snackBar.open('Réservation créée pour l\'employé', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.creationEnCours.set(false);
        this.snackBar.open(err.error?.message ?? 'Erreur lors de la réservation', 'Fermer', { duration: 5000 });
      }
    });
  }

  fermer(): void {
    this.dialogRef.close(this.uneModificationAEteFaite);
  }
}
