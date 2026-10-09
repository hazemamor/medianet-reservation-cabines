package com.medianet.reservation.dto;

import java.util.List;

public record DashboardAdminResponse(
        long totalCabines,
        long cabinesDisponibles,
        long cabinesEnMaintenance,
        long totalReservations,
        long reservationsConfirmees,
        long reservationsAnnulees,
        double tauxUtilisationGlobal,
        List<CabinePopulaire> cabinesLesPlusSollicitees
) {
    public record CabinePopulaire(Long cabineId, String nom, String numero, long nombreReservations) {}
}
