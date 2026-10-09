import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ChatbotRequest, ChatbotResponse } from '../models/chatbot.model';

@Injectable({ providedIn: 'root' })
export class ChatbotService {

  constructor(private http: HttpClient) {}

  demander(message: string): Observable<ChatbotResponse> {
    const request: ChatbotRequest = { message };
    return this.http.post<ChatbotResponse>(`${environment.apiUrl}/chatbot/ask`, request);
  }
}
