package com.medianet.reservation.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatbotRequest(
        @NotBlank String message
) {}
