export interface CabinePopulaire {
  cabineId: number;
  nom: string;
  numero: string;
  nombreReservations: number;
}

export interface DashboardAdminResponse {
  totalCabines: number;
  cabinesDisponibles: number;
  cabinesEnMaintenance: number;
  totalReservations: number;
  reservationsConfirmees: number;
  reservationsAnnulees: number;
  tauxUtilisationGlobal: number;
  cabinesLesPlusSollicitees: CabinePopulaire[];
}
