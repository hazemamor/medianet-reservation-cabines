import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './page-header.component.html'
})
export class PageHeaderComponent {

  @Input() titre = '';
  @Input() sousTitre = '';

  constructor(private authService: AuthService, private router: Router) {}

  deconnexion(): void {
    this.authService.deconnexion();
    this.router.navigate(['/login']);
  }
}
