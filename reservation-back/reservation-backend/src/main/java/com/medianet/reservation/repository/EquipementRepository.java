package com.medianet.reservation.repository;

import com.medianet.reservation.entity.Equipement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {
    Optional<Equipement> findByNomIgnoreCase(String nom);
    List<Equipement> findByNomInIgnoreCase(List<String> noms);
}
