package com.medianet.reservation.repository;

import com.medianet.reservation.entity.Otp;
import com.medianet.reservation.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByUtilisateurOrderByIdDesc(Utilisateur utilisateur);

    void deleteByDateExpirationBefore(LocalDateTime dateTime);
}
