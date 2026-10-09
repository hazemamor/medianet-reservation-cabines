import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Equipement } from '../models/equipement.model';

@Injectable({ providedIn: 'root' })
export class EquipementService {

  constructor(private http: HttpClient) {}

  lister(): Observable<Equipement[]> {
    return this.http.get<Equipement[]>(`${environment.apiUrl}/equipements`);
  }

  // --- Endpoints admin ---

  creer(equipement: Omit<Equipement, 'id'>): Observable<Equipement> {
    return this.http.post<Equipement>(`${environment.apiUrl}/admin/equipements`, equipement);
  }

  modifier(id: number, equipement: Omit<Equipement, 'id'>): Observable<Equipement> {
    return this.http.put<Equipement>(`${environment.apiUrl}/admin/equipements/${id}`, equipement);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/equipements/${id}`);
  }
}
