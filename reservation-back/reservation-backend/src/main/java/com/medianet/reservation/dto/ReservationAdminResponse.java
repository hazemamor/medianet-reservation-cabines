package com.medianet.reservation.dto;

import com.medianet.reservation.entity.enums.StatutReservation;

import java.time.LocalDateTime;

public record ReservationAdminResponse(
        Long id,
        Long cabineId,
        String cabineNom,
        String cabineNumero,
        String employeEmail,
        String employeNom,
        String employePrenom,
        LocalDateTime dateReservation,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        StatutReservation statut,
        Boolean presenceConfirmee
) {}
