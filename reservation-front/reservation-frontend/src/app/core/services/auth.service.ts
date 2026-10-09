import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, VerifyOtpRequest } from '../models/auth.model';

const STORAGE_KEY = 'medianet_auth';

interface StoredAuth {
  token: string;
  email: string;
  role: 'EMPLOYE' | 'ADMINISTRATEUR';
  userId: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  // Signal reactif consulte par les guards/composants (ex: pour afficher/masquer le menu admin)
  readonly currentUser = signal<StoredAuth | null>(this.lireDepuisStockage());

  constructor(private http: HttpClient) {}

  demanderOtp(email: string): Observable<{ message: string }> {
    const body: LoginRequest = { email };
    return this.http.post<{ message: string }>(`${environment.apiUrl}/auth/login`, body);
  }

  verifierOtp(email: string, code: string): Observable<AuthResponse> {
    const body: VerifyOtpRequest = { email, code };
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/verify-otp`, body).pipe(
      tap(res => this.enregistrerSession(res))
    );
  }

  connexionAvecGoogle(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/google`, { idToken }).pipe(
      tap(res => this.enregistrerSession(res))
    );
  }

  deconnexion(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.currentUser.set(null);
  }

  getToken(): string | null {
    return this.currentUser()?.token ?? null;
  }

  isAuthenticated(): boolean {
    return this.currentUser() !== null;
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMINISTRATEUR';
  }

  private enregistrerSession(res: AuthResponse): void {
    const data: StoredAuth = { token: res.token, email: res.email, role: res.role, userId: res.userId };
    // sessionStorage plutot que localStorage : efface a la fermeture de l'onglet,
    // reduit la fenetre d'exposition en cas de XSS. Pour une securite maximale,
    // envisager un cookie HttpOnly pose par le backend (necessite d'adapter le CORS/CSRF).
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    this.currentUser.set(data);
  }

  private lireDepuisStockage(): StoredAuth | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) as StoredAuth : null;
  }
}
