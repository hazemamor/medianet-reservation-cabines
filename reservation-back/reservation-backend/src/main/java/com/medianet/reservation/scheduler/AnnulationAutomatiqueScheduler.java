package com.medianet.reservation.scheduler;

import com.medianet.reservation.entity.Reservation;
import com.medianet.reservation.entity.enums.StatutReservation;
import com.medianet.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Correspond au diagramme de séquence 3.5 "Annulation automatique" :
 * toutes les 5 minutes, recherche les réservations non confirmées dont le
 * délai de 15 minutes après l'heure de début est dépassé, les annule et
 * libère la cabine.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnnulationAutomatiqueScheduler {

    private final ReservationRepository reservationRepository;

    @Value("${app.reservation.confirmation-window-minutes}")
    private int fenetreConfirmationMinutes;

    @Scheduled(cron = "${app.reservation.scheduler-cron}")
    @Transactional
    public void annulerReservationsNonConfirmees() {
        LocalDateTime seuil = LocalDateTime.now().minusMinutes(fenetreConfirmationMinutes);

        List<Reservation> expirees = reservationRepository.findReservationsNonConfirmeesExpirees(seuil, StatutReservation.EN_ATTENTE);

        for (Reservation reservation : expirees) {
            reservation.setStatut(StatutReservation.ANNULEE);
            log.info("Réservation #{} annulée automatiquement (cabine {} libérée)",
                    reservation.getId(), reservation.getCabine().getNumero());
        }

        if (!expirees.isEmpty()) {
            reservationRepository.saveAll(expirees);
        }
    }
}
