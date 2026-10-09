package com.medianet.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull Long cabineId,
        @NotNull @Future LocalDateTime dateDebut,
        @NotNull @Future LocalDateTime dateFin
) {}
