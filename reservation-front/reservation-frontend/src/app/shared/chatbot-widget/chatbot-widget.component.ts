import { Component, ElementRef, ViewChild, AfterViewChecked, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';
import { ChatbotService } from '../../core/services/chatbot.service';
import { ChatbotMessage } from '../../core/models/chatbot.model';

/**
 * Widget de chat flottant, visible sur toutes les pages une fois connecte. Permet de poser des
 * questions en langage naturel sur les cabines, equipements et reservations : le backend
 * (ChatbotService cote serveur) repond en s'appuyant directement sur les donnees reelles du site.
 */
@Component({
  selector: 'app-chatbot-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, MatProgressSpinnerModule],
  templateUrl: './chatbot-widget.component.html',
  styleUrl: './chatbot-widget.component.scss'
})
export class ChatbotWidgetComponent implements AfterViewChecked {

  authService = inject(AuthService);
  private chatbotService = inject(ChatbotService);

  @ViewChild('zoneMessages') private zoneMessages?: ElementRef<HTMLDivElement>;

  ouvert = signal(false);
  enAttente = signal(false);
  saisie = '';

  messages = signal<ChatbotMessage[]>([
    {
      auteur: 'bot',
      texte: 'Bonjour ! Je peux vous renseigner sur les cabines, leurs equipements et les creneaux deja reserves.',
      suggestions: ['Cabines libres aujourd\'hui', 'Cabines reservees demain', 'Liste des equipements']
    }
  ]);

  private doitDefiler = false;

  ngAfterViewChecked(): void {
    if (this.doitDefiler && this.zoneMessages) {
      this.zoneMessages.nativeElement.scrollTop = this.zoneMessages.nativeElement.scrollHeight;
      this.doitDefiler = false;
    }
  }

  basculer(): void {
    this.ouvert.set(!this.ouvert());
  }

  envoyerSuggestion(suggestion: string): void {
    this.saisie = suggestion;
    this.envoyer();
  }

  envoyer(): void {
    const texte = this.saisie.trim();
    if (!texte || this.enAttente()) {
      return;
    }

    this.messages.update(m => [...m, { auteur: 'utilisateur', texte }]);
    this.saisie = '';
    this.enAttente.set(true);
    this.doitDefiler = true;

    this.chatbotService.demander(texte).subscribe({
      next: (reponse) => {
        this.messages.update(m => [...m, { auteur: 'bot', texte: reponse.reponse, suggestions: reponse.suggestions }]);
        this.enAttente.set(false);
        this.doitDefiler = true;
      },
      error: () => {
        this.messages.update(m => [...m, {
          auteur: 'bot',
          texte: 'Désolé, une erreur est survenue. Merci de réessayer dans un instant.'
        }]);
        this.enAttente.set(false);
        this.doitDefiler = true;
      }
    });
  }
}
