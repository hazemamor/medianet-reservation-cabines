import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent {

  ouverte = signal(true);

  constructor(public authService: AuthService, private router: Router) {}

  get initiales(): string {
    const email = this.authService.currentUser()?.email ?? '';
    const partieAvantArobase = email.split('@')[0] ?? '';
    const morceaux = partieAvantArobase.split(/[._-]/).filter(Boolean);
    if (morceaux.length >= 2) {
      return (morceaux[0][0] + morceaux[1][0]).toUpperCase();
    }
    return partieAvantArobase.slice(0, 2).toUpperCase() || '??';
  }

  get libelleRole(): string {
    return this.authService.isAdmin() ? 'Administrateur' : 'Employe';
  }

  basculer(): void {
    this.ouverte.update(v => !v);
  }

  deconnexion(): void {
    this.authService.deconnexion();
    this.router.navigate(['/login']);
  }
}
