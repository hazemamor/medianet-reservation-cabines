import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CabineService } from '../../../core/services/cabine.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { Cabine, CreneauOccupe } from '../../../core/models/cabine.model';
import { dateNonPasseeValidator, heureDebutAvantFinValidator } from '../../../core/validators/reservation-validators';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-cabine-detail',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule, MatCardModule, MatChipsModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatDatepickerModule, MatProgressSpinnerModule,
    MatSnackBarModule, PageHeaderComponent
  ],
  templateUrl: './cabine-detail.component.html'
})
export class CabineDetailComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private cabineService = inject(CabineService);
  private reservationService = inject(ReservationService);
  private snackBar = inject(MatSnackBar);

  cabine = signal<Cabine | null>(null);
  chargement = signal(true);
  reservationEnCours = signal(false);

  /** Créneaux déjà réservés de la cabine pour la date sélectionnée, pour aider à choisir un horaire libre. */
  creneauxOccupes = signal<CreneauOccupe[]>([]);
  chargementCreneaux = signal(false);

  readonly filtreDateMin = (date: Date | null): boolean => {
    if (!date) return true;
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);
    return date.getTime() >= aujourdhui.getTime();
  };

  reservationForm = this.fb.group({
    dateJour: [null as Date | null, [Validators.required, dateNonPasseeValidator()]],
    heureDebut: ['09:00', Validators.required],
    heureFin: ['10:00', Validators.required]
  }, { validators: heureDebutAvantFinValidator('heureDebut', 'heureFin') });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cabineService.obtenir(id).subscribe({
      next: (c) => {
        this.cabine.set(c);
        this.chargement.set(false);
        this.reservationForm.patchValue({
          heureDebut: c.heureDisponibiliteDebut?.substring(0, 5) ?? '09:00',
          heureFin: c.heureDisponibiliteFin?.substring(0, 5) ?? '10:00'
        });
      },
      error: () => {
        this.chargement.set(false);
        this.snackBar.open('Cabine introuvable', 'Fermer', { duration: 4000 });
        this.router.navigate(['/cabines']);
      }
    });

    this.reservationForm.get('dateJour')!.valueChanges.subscribe((date) => this.chargerCreneauxOccupes(date));
  }

  private chargerCreneauxOccupes(date: Date | null): void {
    const c = this.cabine();
    if (!c || !date) {
      this.creneauxOccupes.set([]);
      return;
    }
    this.chargementCreneaux.set(true);
    this.cabineService.creneauxOccupes(c.id, this.formatDateLocale(date)).subscribe({
      next: (creneaux) => {
        this.creneauxOccupes.set(creneaux);
        this.chargementCreneaux.set(false);
      },
      error: () => {
        this.creneauxOccupes.set([]);
        this.chargementCreneaux.set(false);
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

  // --- Frise horaire (position en % des créneaux occupés / de la sélection en cours) ---

  private minutesDe(heure: string): number {
    const [h, m] = heure.substring(0, 5).split(':').map(Number);
    return h * 60 + m;
  }

  private dureeTotaleCabineMinutes(): number {
    const c = this.cabine();
    if (!c) return 1;
    return Math.max(1, this.minutesDe(c.heureDisponibiliteFin) - this.minutesDe(c.heureDisponibiliteDebut));
  }

  pourcentage(heure: string): number {
    const c = this.cabine();
    if (!c) return 0;
    const pct = ((this.minutesDe(heure) - this.minutesDe(c.heureDisponibiliteDebut)) / this.dureeTotaleCabineMinutes()) * 100;
    return Math.min(100, Math.max(0, pct));
  }

  largeurSegment(heureDebut: string, heureFin: string): number {
    return Math.max(0, this.pourcentage(heureFin) - this.pourcentage(heureDebut));
  }

  /** true si le créneau actuellement saisi par l'employé chevauche un créneau déjà réservé. */
  get selectionChevaucheCreneauOccupe(): boolean {
    const v = this.reservationForm.value;
    if (!v.heureDebut || !v.heureFin || v.heureDebut >= v.heureFin) return false;
    return this.creneauxOccupes().some(cr => {
      const debut = cr.heureDebut.substring(0, 5);
      const fin = cr.heureFin.substring(0, 5);
      return v.heureDebut! < fin && v.heureFin! > debut;
    });
  }

  private combinerDateHeure(jour: Date, heure: string): string {
    const [h, m] = heure.split(':');
    return `${this.formatDateLocale(jour)}T${h.padStart(2, '0')}:${m.padStart(2, '0')}:00`;
  }

  get erreurDatePassee(): boolean {
    return this.reservationForm.get('dateJour')?.hasError('dateDansLePasse') ?? false;
  }

  get erreurHeures(): boolean {
    return this.reservationForm.hasError('heureDebutApresFin');
  }

  /** Verifie que les heures saisies restent dans la plage de disponibilite de la cabine. */
  get erreurHorsPlage(): boolean {
    const c = this.cabine();
    const v = this.reservationForm.value;
    if (!c || !v.heureDebut || !v.heureFin) return false;
    const debutCabine = c.heureDisponibiliteDebut.substring(0, 5);
    const finCabine = c.heureDisponibiliteFin.substring(0, 5);
    return v.heureDebut < debutCabine || v.heureFin > finCabine;
  }

  reserver(): void {
    this.reservationForm.markAllAsTouched();
    const c = this.cabine();
    const v = this.reservationForm.value;

    if (this.reservationForm.invalid || !c || !v.dateJour || !v.heureDebut || !v.heureFin) {
      this.snackBar.open('Verifie la date et les heures saisies', 'Fermer', { duration: 4000 });
      return;
    }
    if (this.erreurHorsPlage) {
      this.snackBar.open(
        `Cette cabine n'est disponible que de ${c.heureDisponibiliteDebut.substring(0, 5)} a ${c.heureDisponibiliteFin.substring(0, 5)}`,
        'Fermer', { duration: 5000 }
      );
      return;
    }
    if (this.selectionChevaucheCreneauOccupe) {
      this.snackBar.open('Ce creneau chevauche une reservation existante, choisis un creneau libre', 'Fermer', { duration: 5000 });
      return;
    }

    const dateDebut = this.combinerDateHeure(v.dateJour, v.heureDebut);
    const dateFin = this.combinerDateHeure(v.dateJour, v.heureFin);

    this.reservationEnCours.set(true);
    this.reservationService.reserver({ cabineId: c.id, dateDebut, dateFin }).subscribe({
      next: () => {
        this.reservationEnCours.set(false);
        this.snackBar.open(`Cabine ${c.nom} reservee avec succes`, 'OK', { duration: 4000 });
        this.router.navigate(['/reservations/historique']);
      },
      error: (err) => {
        this.reservationEnCours.set(false);
        this.snackBar.open(err.error?.message ?? 'Erreur lors de la reservation', 'Fermer', { duration: 5000 });
      }
    });
  }
}
