export type EtatCabine = 'DISPONIBLE' | 'RESERVEE' | 'MAINTENANCE';

export interface Cabine {
  id: number;
  nom: string;
  numero: string;
  capacite: number;
  description: string;
  localisation: string;
  etat: EtatCabine;
  equipements: string[];
  /** Heure d'ouverture de la cabine aux reservations, format "HH:mm:ss" (ex: "09:00:00"). */
  heureDisponibiliteDebut: string;
  /** Heure de fermeture de la cabine aux reservations, format "HH:mm:ss". */
  heureDisponibiliteFin: string;
  /** URL de la photo illustrant la cabine (peut etre vide/null). */
  photoUrl: string | null;
}

export interface RechercheCabineRequest {
  nombrePersonnes?: number;
  capaciteMin?: number;
  equipements?: string[];
  dateDebut?: string; // format ISO LocalDateTime, ex: 2026-07-20T09:00:00
  dateFin?: string;
}

/** Un créneau déjà réservé sur une cabine pour un jour donné (sans identité de l'employé). */
export interface CreneauOccupe {
  heureDebut: string; // format "HH:mm:ss"
  heureFin: string;
}
