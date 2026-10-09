import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Cabine, CreneauOccupe, RechercheCabineRequest } from '../models/cabine.model';

@Injectable({ providedIn: 'root' })
export class CabineService {

  constructor(private http: HttpClient) {}

  lister(): Observable<Cabine[]> {
    return this.http.get<Cabine[]>(`${environment.apiUrl}/cabines`);
  }

  obtenir(id: number): Observable<Cabine> {
    return this.http.get<Cabine>(`${environment.apiUrl}/cabines/${id}`);
  }

  rechercher(criteres: RechercheCabineRequest): Observable<Cabine[]> {
    return this.http.post<Cabine[]>(`${environment.apiUrl}/cabines/recherche`, criteres);
  }

  /** Créneaux déjà réservés de la cabine pour un jour donné (yyyy-MM-dd), pour aider au choix d'un créneau libre. */
  creneauxOccupes(id: number, dateIso: string): Observable<CreneauOccupe[]> {
    return this.http.get<CreneauOccupe[]>(`${environment.apiUrl}/cabines/${id}/creneaux-occupes`, {
      params: { date: dateIso }
    });
  }

  // --- Endpoints admin ---

  creer(cabine: Omit<Cabine, 'id'>): Observable<Cabine> {
    return this.http.post<Cabine>(`${environment.apiUrl}/admin/cabines`, cabine);
  }

  modifier(id: number, cabine: Omit<Cabine, 'id'>): Observable<Cabine> {
    return this.http.put<Cabine>(`${environment.apiUrl}/admin/cabines/${id}`, cabine);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/cabines/${id}`);
  }

  mettreEnMaintenance(id: number): Observable<Cabine> {
    return this.http.patch<Cabine>(`${environment.apiUrl}/admin/cabines/${id}/maintenance`, {});
  }

  remettreDisponible(id: number): Observable<Cabine> {
    return this.http.patch<Cabine>(`${environment.apiUrl}/admin/cabines/${id}/disponible`, {});
  }
}
