import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';

import { DashboardService } from '../../../core/services/dashboard.service';
import { AdminReservationService } from '../../../core/services/admin-reservation.service';
import { DashboardAdminResponse } from '../../../core/models/dashboard.model';
import { ReservationAdminResponse } from '../../../core/models/reservation-admin.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

const PALETTE_COULEURS = [
  '#8b5cf6',
  '#0AADDA',
  '#F5A623',
  '#22c55e',
  '#ef4444',
  '#ec4899',
  '#14b8a6',
  '#f97316'
];

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatProgressSpinnerModule,
    MatTableModule,
    PageHeaderComponent
  ],
  templateUrl: './admin-dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private adminReservationService = inject(AdminReservationService);

  stats = signal<DashboardAdminResponse | null>(null);
  activiteRecente = signal<ReservationAdminResponse[]>([]);
  chargement = signal(true);
  colonnesPopulaires = [
  'nom',
  'numero',
  'nombreReservations'
];

  /** Circonference du cercle SVG (rayon 40) pour le donut du taux d'utilisation. */
  readonly circonference = 2 * Math.PI * 40;

  offsetDonut = computed(() => {
    const taux = this.stats()?.tauxUtilisationGlobal ?? 0;
    return this.circonference - (Math.min(taux, 100) / 100) * this.circonference;
  });

  cabinesMaxReservations = computed(() => {
    const liste = this.stats()?.cabinesLesPlusSollicitees ?? [];
    return Math.max(1, ...liste.map(c => c.nombreReservations));
  });

  ngOnInit(): void {
    this.chargement.set(true);
    this.dashboardService.getStatistiques().subscribe({
      next: (data) => { this.stats.set(data); this.chargement.set(false); },
      error: () => this.chargement.set(false)
    });
    this.adminReservationService.listerToutes().subscribe(data => {
      const triees = [...data].sort((a, b) => b.dateReservation.localeCompare(a.dateReservation));
      this.activiteRecente.set(triees.slice(0, 5));
    });
  }

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
}
