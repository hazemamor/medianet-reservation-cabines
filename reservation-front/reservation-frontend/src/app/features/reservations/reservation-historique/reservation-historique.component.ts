import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ReservationService } from '../../../core/services/reservation.service';
import { ReservationResponse } from '../../../core/models/reservation.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

type FiltreStatut = 'TOUTES' | 'EN_ATTENTE' | 'CONFIRMEE' | 'ANNULEE';

const PALETTE_COULEURS = ['#8b5cf6', '#0AADDA', '#F5A623', '#22c55e', '#ef4444', '#ec4899', '#14b8a6', '#f97316'];

@Component({
  selector: 'app-reservation-historique',
  standalone: true,
  imports: [CommonModule, MatSnackBarModule, MatProgressSpinnerModule, PageHeaderComponent],
  templateUrl: './reservation-historique.component.html'
})
export class ReservationHistoriqueComponent implements OnInit {

  private reservationService = inject(ReservationService);
  private snackBar = inject(MatSnackBar);

  reservations = signal<ReservationResponse[]>([]);
  chargement = signal(true);
  actionEnCours = signal<number | null>(null);
  filtre = signal<FiltreStatut>('TOUTES');

  compteurs = computed(() => {
    const liste = this.reservations();
    return {
      total: liste.length,
      confirmees: liste.filter(r => r.statut === 'CONFIRMEE').length,
      annulees: liste.filter(r => r.statut === 'ANNULEE').length,
      enAttente: liste.filter(r => r.statut === 'EN_ATTENTE').length
    };
  });

  reservationsFiltrees = computed(() => {
    const f = this.filtre();
    if (f === 'TOUTES') return this.reservations();
    return this.reservations().filter(r => r.statut === f);
  });

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement.set(true);
    this.reservationService.historique().subscribe({
      next: (data) => { this.reservations.set(data); this.chargement.set(false); },
      error: () => this.chargement.set(false)
    });
  }

  changerFiltre(f: FiltreStatut): void {
    this.filtre.set(f);
  }

  /** Couleur deterministe par numero de cabine, pour le badge dans le tableau. */
  couleurCabine(numero: string): string {
    let hash = 0;
    for (let i = 0; i < numero.length; i++) hash = numero.charCodeAt(i) + ((hash << 5) - hash);
    return PALETTE_COULEURS[Math.abs(hash) % PALETTE_COULEURS.length];
  }

  statutLabel(statut: string): string {
    switch (statut) {
      case 'CONFIRMEE': return 'Confirmée';
      case 'ANNULEE': return 'Annulée';
      case 'EN_ATTENTE': return 'En attente';
      default: return statut;
    }
  }

  statutClasse(statut: string): string {
    switch (statut) {
      case 'CONFIRMEE': return 'badge-statut--dispo';
      case 'ANNULEE': return 'badge-statut--maintenance';
      case 'EN_ATTENTE': return 'badge-statut--occupee';
      default: return '';
    }
  }

  duree(dateDebut: string, dateFin: string): string {
    const mins = Math.round((new Date(dateFin).getTime() - new Date(dateDebut).getTime()) / 60000);
    if (mins <= 0) return '—';
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return h > 0 ? `${h}h${m > 0 ? m + 'm' : ''}` : `${m}m`;
  }

  peutConfirmerPresence(reservation: ReservationResponse): boolean {
    return reservation.statut === 'EN_ATTENTE' && !reservation.presenceConfirmee;
  }

  peutAnnuler(reservation: ReservationResponse): boolean {
    return reservation.statut === 'EN_ATTENTE' && new Date(reservation.dateDebut) > new Date();
  }

  confirmerPresence(reservation: ReservationResponse): void {
    this.actionEnCours.set(reservation.id);
    this.reservationService.confirmerPresence(reservation.id).subscribe({
      next: () => {
        this.actionEnCours.set(null);
        this.snackBar.open('Presence confirmee', 'OK', { duration: 3000 });
        this.charger();
      },
      error: (err) => {
        this.actionEnCours.set(null);
        this.snackBar.open(err.error?.message ?? 'Erreur', 'Fermer', { duration: 5000 });
      }
    });
  }

  annuler(reservation: ReservationResponse): void {
    this.actionEnCours.set(reservation.id);
    this.reservationService.annuler(reservation.id).subscribe({
      next: () => {
        this.actionEnCours.set(null);
        this.snackBar.open('Reservation annulee', 'OK', { duration: 3000 });
        this.charger();
      },
      error: (err) => {
        this.actionEnCours.set(null);
        this.snackBar.open(err.error?.message ?? 'Erreur', 'Fermer', { duration: 5000 });
      }
    });
  }
}
