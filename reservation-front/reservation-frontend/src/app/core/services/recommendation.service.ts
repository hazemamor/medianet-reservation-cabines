import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RecommendationResponse } from '../models/recommendation.model';

@Injectable({ providedIn: 'root' })
export class RecommendationService {

  constructor(private http: HttpClient) {}

  recommander(nombreParticipants: number, equipements: string[], dateDebut: string, dateFin: string): Observable<RecommendationResponse> {
    let params = `nombreParticipants=${nombreParticipants}&dateDebut=${encodeURIComponent(dateDebut)}&dateFin=${encodeURIComponent(dateFin)}`;
    equipements.forEach(eq => params += `&equipements=${encodeURIComponent(eq)}`);
    return this.http.get<RecommendationResponse>(`${environment.apiUrl}/recommandations?${params}`);
  }
}
