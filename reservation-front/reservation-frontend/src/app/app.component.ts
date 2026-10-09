import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from './shared/sidebar/sidebar.component';
import { ChatbotWidgetComponent } from './shared/chatbot-widget/chatbot-widget.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, ChatbotWidgetComponent],
  template: `
    <div class="app-shell">
      <app-sidebar></app-sidebar>
      <main class="app-main">
        <router-outlet></router-outlet>
      </main>
    </div>
    <app-chatbot-widget></app-chatbot-widget>
  `
})
export class AppComponent {}
