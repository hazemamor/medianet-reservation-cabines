package com.medianet.reservation.repository;

import com.medianet.reservation.entity.Utilisateur;
import com.medianet.reservation.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Utilisateur> findByRoleOrderByNomAscPrenomAsc(Role role);
}
