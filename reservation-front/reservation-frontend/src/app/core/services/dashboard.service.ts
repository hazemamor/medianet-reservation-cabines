import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardAdminResponse } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {

  constructor(private http: HttpClient) {}

  getStatistiques(): Observable<DashboardAdminResponse> {
    return this.http.get<DashboardAdminResponse>(`${environment.apiUrl}/admin/dashboard`);
  }
}
