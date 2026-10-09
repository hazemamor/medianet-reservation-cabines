import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CabineService } from '../../../core/services/cabine.service';
import { AdminReservationService } from '../../../core/services/admin-reservation.service';
import { Cabine } from '../../../core/models/cabine.model';
import { ReservationAdminResponse } from '../../../core/models/reservation-admin.model';
import { JourDetailDialogComponent, JourDetailDialogData } from './jour-detail-dialog/jour-detail-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

type StatutCellule = 'DISPONIBLE' | 'PARTIEL' | 'COMPLET' | 'MAINTENANCE';

interface Cellule {
  date: Date;
  statut: StatutCellule;
  nombreReservations: number;
}

@Component({
  selector: 'app-admin-disponibilite',
  standalone: true,
  imports: [
    CommonModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule, MatDialogModule, PageHeaderComponent
  ],
  templateUrl: './admin-disponibilite.component.html'
})
export class AdminDisponibiliteComponent implements OnInit {

  private cabineService = inject(CabineService);
  private adminReservationService = inject(AdminReservationService);
  private dialog = inject(MatDialog);

  cabines = signal<Cabine[]>([]);
  toutesLesReservations = signal<ReservationAdminResponse[]>([]);
  chargement = signal(true);

  dateDebutPlage = signal<Date>(this.debutSemaine());
  nombreJours = 14;

  /** Colonnes de la grille : une par jour, sur la plage affichee. */
  jours = computed<Date[]>(() => {
    const debut = this.dateDebutPlage();
    return Array.from({ length: this.nombreJours }, (_, i) => {
      const d = new Date(debut);
      d.setDate(d.getDate() + i);
      return d;
    });
  });

  ngOnInit(): void {
    this.charger();
  }

  private debutSemaine(): Date {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  }

  private charger(): void {
    this.chargement.set(true);
    this.cabineService.lister().subscribe(cabines => this.cabines.set(cabines));
    this.adminReservationService.listerToutes().subscribe({
      next: (data) => { this.toutesLesReservations.set(data); this.chargement.set(false); },
      error: () => this.chargement.set(false)
    });
  }

  reculer(): void {
    const d = new Date(this.dateDebutPlage());
    d.setDate(d.getDate() - this.nombreJours);
    this.dateDebutPlage.set(d);
  }

  avancer(): void {
    const d = new Date(this.dateDebutPlage());
    d.setDate(d.getDate() + this.nombreJours);
    this.dateDebutPlage.set(d);
  }

  revenirAujourdhui(): void {
    this.dateDebutPlage.set(this.debutSemaine());
  }

  /**
   * Construit la clé "yyyy-MM-dd" d'un jour à partir de ses composants LOCAUX (et non
   * date.toISOString(), qui convertit en UTC : pour un fuseau horaire en avance sur UTC comme
   * la Tunisie (UTC+1), minuit local devient la veille en UTC, ce qui décalait toute la grille
   * d'un jour — une réservation créée pour le 28/08 apparaissait dans la colonne du 29/08).
   */
  private cleJour(date: Date): string {
    const annee = date.getFullYear();
    const mois = String(date.getMonth() + 1).padStart(2, '0');
    const jour = String(date.getDate()).padStart(2, '0');
    return `${annee}-${mois}-${jour}`;
  }

  private reservationsPour(cabineId: number, date: Date): ReservationAdminResponse[] {
    const cle = this.cleJour(date);
    return this.toutesLesReservations().filter(r =>
      r.cabineId === cabineId && r.dateDebut.substring(0, 10) === cle && r.statut !== 'ANNULEE'
    );
  }

  /** Duree totale (en minutes) de la plage de disponibilite quotidienne de la cabine. */
  private dureeDisponibiliteMinutes(cabine: Cabine): number {
    const [hD, mD] = cabine.heureDisponibiliteDebut.substring(0, 5).split(':').map(Number);
    const [hF, mF] = cabine.heureDisponibiliteFin.substring(0, 5).split(':').map(Number);
    return Math.max(0, (hF * 60 + mF) - (hD * 60 + mD));
  }

  /** Somme des durees (en minutes) des reservations donnees (les creneaux ne se chevauchent jamais). */
  private dureeReserveeMinutes(reservations: ReservationAdminResponse[]): number {
    return reservations.reduce((total, r) => {
      const debut = new Date(r.dateDebut).getTime();
      const fin = new Date(r.dateFin).getTime();
      return total + Math.max(0, (fin - debut) / 60000);
    }, 0);
  }

  cellule(cabine: Cabine, date: Date): Cellule {
    if (cabine.etat === 'MAINTENANCE') {
      return { date, statut: 'MAINTENANCE', nombreReservations: 0 };
    }
    const reservations = this.reservationsPour(cabine.id, date);
    if (reservations.length === 0) {
      return { date, statut: 'DISPONIBLE', nombreReservations: 0 };
    }
    // "Complet" = la duree totale reservee couvre (au moins) toute la plage de
    // disponibilite de la cabine ce jour-la — peu importe que ce soit une seule
    // longue reservation ou plusieurs creneaux courts mis bout a bout.
    const dureeTotale = this.dureeDisponibiliteMinutes(cabine);
    const dureeReservee = this.dureeReserveeMinutes(reservations);
    const statut: StatutCellule = dureeTotale > 0 && dureeReservee >= dureeTotale ? 'COMPLET' : 'PARTIEL';
    return { date, statut, nombreReservations: reservations.length };
  }

  ouvrirDetail(cabine: Cabine, date: Date): void {
    if (cabine.etat === 'MAINTENANCE') {
      return;
    }
    const reservations = this.reservationsPour(cabine.id, date);
    const data: JourDetailDialogData = {
      cabineId: cabine.id,
      cabineNom: cabine.nom,
      cabineNumero: cabine.numero,
      date,
      heureDisponibiliteDebut: cabine.heureDisponibiliteDebut,
      heureDisponibiliteFin: cabine.heureDisponibiliteFin,
      reservations
    };

    const ref = this.dialog.open(JourDetailDialogComponent, { width: '520px', data });
    ref.afterClosed().subscribe(uneModificationAEteFaite => {
      if (uneModificationAEteFaite) {
        this.charger();
      }
    });
  }
}
