import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ReservationRequest, ReservationResponse } from '../models/reservation.model';

@Injectable({ providedIn: 'root' })
export class ReservationService {

  constructor(private http: HttpClient) {}

  reserver(request: ReservationRequest): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${environment.apiUrl}/reservations`, request);
  }

  confirmerPresence(id: number): Observable<ReservationResponse> {
    return this.http.patch<ReservationResponse>(`${environment.apiUrl}/reservations/${id}/confirmer-presence`, {});
  }

  annuler(id: number): Observable<ReservationResponse> {
    return this.http.patch<ReservationResponse>(`${environment.apiUrl}/reservations/${id}/annuler`, {});
  }

  historique(): Observable<ReservationResponse[]> {
    return this.http.get<ReservationResponse[]>(`${environment.apiUrl}/reservations/historique`);
  }
}
