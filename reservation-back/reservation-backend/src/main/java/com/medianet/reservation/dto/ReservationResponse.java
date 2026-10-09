package com.medianet.reservation.dto;

import com.medianet.reservation.entity.enums.StatutReservation;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long cabineId,
        String cabineNom,
        String cabineNumero,
        LocalDateTime dateReservation,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        StatutReservation statut,
        Boolean presenceConfirmee
) {}
