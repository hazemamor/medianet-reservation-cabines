import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'cabines', pathMatch: 'full' },

  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },

  // Accessibles a tout utilisateur connecte (EMPLOYE ou ADMINISTRATEUR reserve desormais aussi des cabines)
  {
    path: 'cabines',
    canActivate: [authGuard],
    loadComponent: () => import('./features/cabines/cabine-list/cabine-list.component').then(m => m.CabineListComponent)
  },
  {
    path: 'cabines/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/cabines/cabine-detail/cabine-detail.component').then(m => m.CabineDetailComponent)
  },
  {
    path: 'reservations/historique',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reservations/reservation-historique/reservation-historique.component').then(m => m.ReservationHistoriqueComponent)
  },

  {
    path: 'admin/cabines',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin-cabines/admin-cabines.component').then(m => m.AdminCabinesComponent)
  },
  {
    path: 'admin/equipements',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin-equipements/admin-equipements.component').then(m => m.AdminEquipementsComponent)
  },
  {
    path: 'admin/dashboard',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
  },
  {
    path: 'admin/disponibilite',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin-disponibilite/admin-disponibilite.component').then(m => m.AdminDisponibiliteComponent)
  },

  { path: '**', redirectTo: 'cabines' }
];
