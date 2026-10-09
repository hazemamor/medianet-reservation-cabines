export interface ChatbotRequest {
  message: string;
}

export interface ChatbotResponse {
  reponse: string;
  suggestions: string[];
}

/** Message affiché dans la fenêtre de discussion (coté client uniquement). */
export interface ChatbotMessage {
  auteur: 'utilisateur' | 'bot';
  texte: string;
  suggestions?: string[];
}
