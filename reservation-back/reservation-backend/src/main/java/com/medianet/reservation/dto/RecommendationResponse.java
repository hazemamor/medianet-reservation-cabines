package com.medianet.reservation.dto;

public record RecommendationResponse(
        Long cabineId,
        String cabineNom,
        Double score,
        String justification
) {}
