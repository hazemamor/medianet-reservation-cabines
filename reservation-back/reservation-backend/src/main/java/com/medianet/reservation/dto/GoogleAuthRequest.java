package com.medianet.reservation.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String idToken
) {}
