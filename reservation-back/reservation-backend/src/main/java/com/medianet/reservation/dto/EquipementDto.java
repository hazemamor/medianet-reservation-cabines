package com.medianet.reservation.dto;

import jakarta.validation.constraints.NotBlank;

public record EquipementDto(
        Long id,
        @NotBlank String nom,
        String description
) {}
