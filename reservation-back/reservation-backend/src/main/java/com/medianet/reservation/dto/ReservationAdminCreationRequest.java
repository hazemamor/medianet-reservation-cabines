package com.medianet.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Requête utilisée par l'administrateur pour réserver une cabine au nom d'un employé
 * (ex: depuis la grille de disponibilité, en cliquant sur une case libre).
 */
public record ReservationAdminCreationRequest(
        @NotNull Long utilisateurId,
        @NotNull Long cabineId,
        @NotNull @Future LocalDateTime dateDebut,
        @NotNull @Future LocalDateTime dateFin
) {}
