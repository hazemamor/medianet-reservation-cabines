package com.medianet.reservation.service;

import com.medianet.reservation.dto.DashboardAdminResponse;
import com.medianet.reservation.entity.Cabine;
import com.medianet.reservation.entity.enums.EtatCabine;
import com.medianet.reservation.entity.enums.StatutReservation;
import com.medianet.reservation.repository.CabineRepository;
import com.medianet.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Fournit les statistiques du tableau de bord administrateur (6.10.2) :
 * taux d'utilisation, cabines les plus sollicitées, statistiques globales,
 * performance du moteur de recommandation.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CabineRepository cabineRepository;
    private final ReservationRepository reservationRepository;

    public DashboardAdminResponse getStatistiquesAdmin() {
        List<Cabine> cabines = cabineRepository.findAll();
        long totalCabines = cabines.size();
        long cabinesDisponibles = cabines.stream().filter(c -> c.getEtat() == EtatCabine.DISPONIBLE).count();
        long cabinesEnMaintenance = cabines.stream().filter(c -> c.getEtat() == EtatCabine.MAINTENANCE).count();

        long totalReservations = reservationRepository.count();
        long reservationsConfirmees = reservationRepository.countByStatut(StatutReservation.CONFIRMEE);
        long reservationsAnnulees = reservationRepository.countByStatut(StatutReservation.ANNULEE);

        double tauxUtilisation = totalReservations == 0 ? 0.0
                : (double) reservationsConfirmees / totalReservations * 100.0;

        Map<Long, Cabine> cabinesById = cabines.stream()
                .collect(java.util.stream.Collectors.toMap(Cabine::getId, c -> c));

        List<DashboardAdminResponse.CabinePopulaire> cabinesPopulaires = reservationRepository
                .countReservationsParCabine(StatutReservation.ANNULEE)
                .stream()
                .limit(5)
                .map(row -> {
                    Long cabineId = (Long) row[0];
                    Long count = (Long) row[1];
                    Cabine cabine = cabinesById.get(cabineId);
                    return new DashboardAdminResponse.CabinePopulaire(
                            cabineId,
                            cabine != null ? cabine.getNom() : "N/A",
                            cabine != null ? cabine.getNumero() : "N/A",
                            count
                    );
                })
                .sorted(Comparator.comparingLong(DashboardAdminResponse.CabinePopulaire::nombreReservations).reversed())
                .toList();

        return new DashboardAdminResponse(
                totalCabines, cabinesDisponibles, cabinesEnMaintenance,
                totalReservations, reservationsConfirmees, reservationsAnnulees,
                Math.round(tauxUtilisation * 100.0) / 100.0,
                cabinesPopulaires
        );
    }
}
