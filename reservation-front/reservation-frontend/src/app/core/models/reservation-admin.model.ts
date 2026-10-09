import { StatutReservation } from './reservation.model';

export interface ReservationAdminResponse {
  id: number;
  cabineId: number;
  cabineNom: string;
  cabineNumero: string;
  employeEmail: string;
  employeNom: string;
  employePrenom: string;
  dateReservation: string;
  dateDebut: string;
  dateFin: string;
  statut: StatutReservation;
  presenceConfirmee: boolean;
}

/** Employe pouvant etre selectionne par l'admin pour reserver une cabine en son nom. */
export interface UtilisateurResume {
  id: number;
  nom: string;
  prenom: string;
  email: string;
}

/** Requete envoyee par l'admin pour creer une reservation au nom d'un employe. */
export interface ReservationAdminCreationRequest {
  utilisateurId: number;
  cabineId: number;
  dateDebut: string; // format ISO LocalDateTime, ex: 2026-07-20T09:00:00
  dateFin: string;
}
