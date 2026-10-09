export type StatutReservation = 'EN_ATTENTE' | 'CONFIRMEE' | 'ANNULEE';

export interface ReservationRequest {
  cabineId: number;
  dateDebut: string;
  dateFin: string;
}

export interface ReservationResponse {
  id: number;
  cabineId: number;
  cabineNom: string;
  cabineNumero: string;
  dateReservation: string;
  dateDebut: string;
  dateFin: string;
  statut: StatutReservation;
  presenceConfirmee: boolean;
}
