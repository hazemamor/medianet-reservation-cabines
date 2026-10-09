package com.medianet.reservation.repository;

import com.medianet.reservation.entity.Cabine;
import com.medianet.reservation.entity.enums.EtatCabine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CabineRepository extends JpaRepository<Cabine, Long>, JpaSpecificationExecutor<Cabine> {
    Optional<Cabine> findByNumero(String numero);
    List<Cabine> findByEtat(EtatCabine etat);

    /**
     * Verrou pessimiste (SELECT ... FOR UPDATE) posé sur la ligne de la cabine.
     * Utilisé lors de la création d'une réservation : la transaction qui obtient
     * le verrou en premier empêche toute autre transaction concurrente de lire/écrire
     * la même cabine tant qu'elle n'est pas terminée (commit/rollback), ce qui
     * élimine la fenêtre de "race condition" entre la vérification de disponibilité
     * et la création de la réservation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cabine c WHERE c.id = :id")
    Optional<Cabine> findByIdForUpdate(@Param("id") Long id);
}
