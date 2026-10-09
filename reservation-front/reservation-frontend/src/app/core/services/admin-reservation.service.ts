import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ReservationAdminCreationRequest, ReservationAdminResponse, UtilisateurResume } from '../models/reservation-admin.model';

@Injectable({ providedIn: 'root' })
export class AdminReservationService {

  constructor(private http: HttpClient) {}

  listerToutes(): Observable<ReservationAdminResponse[]> {
    return this.http.get<ReservationAdminResponse[]>(`${environment.apiUrl}/admin/reservations`);
  }

  annuler(id: number): Observable<ReservationAdminResponse> {
    return this.http.patch<ReservationAdminResponse>(`${environment.apiUrl}/admin/reservations/${id}/annuler`, {});
  }

  /** Liste des employes, utilisee pour peupler le selecteur lors d'une reservation manuelle. */
  listerEmployes(): Observable<UtilisateurResume[]> {
    return this.http.get<UtilisateurResume[]>(`${environment.apiUrl}/admin/reservations/employes`);
  }

  /** Cree une reservation au nom d'un employe (depuis la grille de disponibilite). */
  reserverPourEmploye(request: ReservationAdminCreationRequest): Observable<ReservationAdminResponse> {
    return this.http.post<ReservationAdminResponse>(`${environment.apiUrl}/admin/reservations`, request);
  }
}
