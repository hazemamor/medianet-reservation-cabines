package com.medianet.reservation.repository;

import com.medianet.reservation.entity.Cabine;
import com.medianet.reservation.entity.Reservation;
import com.medianet.reservation.entity.Utilisateur;
import com.medianet.reservation.entity.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUtilisateurOrderByDateDebutDesc(Utilisateur utilisateur);

    List<Reservation> findAllByOrderByDateDebutDesc();

    List<Reservation> findByCabineAndStatutNot(Cabine cabine, StatutReservation statut);

    /**
     * Vérifie s'il existe un conflit de créneau pour une cabine donnée
     * (chevauchement d'intervalles) sur les réservations non annulées.
     * L'enum est passée en paramètre (:statutAnnulee) plutôt qu'écrite en dur
     * dans le JPQL, pour éviter toute dépendance au nom exact du package.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.cabine.id = :cabineId
              AND r.statut <> :statutAnnulee
              AND r.dateDebut < :dateFin
              AND r.dateFin > :dateDebut
            """)
    boolean existsConflict(@Param("cabineId") Long cabineId,
                            @Param("dateDebut") LocalDateTime dateDebut,
                            @Param("dateFin") LocalDateTime dateFin,
                            @Param("statutAnnulee") StatutReservation statutAnnulee);

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.statut = :statutEnAttente
              AND r.presenceConfirmee = false
              AND r.dateDebut <= :seuil
            """)
    List<Reservation> findReservationsNonConfirmeesExpirees(@Param("seuil") LocalDateTime seuil,
                                                              @Param("statutEnAttente") StatutReservation statutEnAttente);

    @Query("""
            SELECT r.cabine.id, COUNT(r) FROM Reservation r
            WHERE r.statut <> :statutAnnulee
            GROUP BY r.cabine.id
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> countReservationsParCabine(@Param("statutAnnulee") StatutReservation statutAnnulee);

    long countByStatut(StatutReservation statut);
}
