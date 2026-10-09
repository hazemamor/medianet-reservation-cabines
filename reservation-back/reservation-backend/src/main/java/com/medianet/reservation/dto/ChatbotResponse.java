package com.medianet.reservation.dto;

import java.util.List;

public record ChatbotResponse(
        String reponse,
        List<String> suggestions
) {
    public ChatbotResponse(String reponse) {
        this(reponse, List.of());
    }
}
