import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Ne pas attacher le token sur les endpoints publics d'authentification
  const estRoutePublique = req.url.includes('/auth/login') || req.url.includes('/auth/verify-otp');

  if (token && !estRoutePublique) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req);
};
