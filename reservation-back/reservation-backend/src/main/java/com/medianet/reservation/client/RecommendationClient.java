package com.medianet.reservation.client;

import com.medianet.reservation.dto.RecommendationRequest;
import com.medianet.reservation.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Communique avec le microservice IA (FastAPI + Random Forest) - voir diagramme
 * de séquence 3.4 "Recommandation IA" et l'architecture logique/physique.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationClient {

    private final WebClient iaWebClient;

    public Optional<RecommendationResponse> obtenirRecommandation(RecommendationRequest request) {
        try {
            RecommendationResponse response = iaWebClient.post()
                    .uri("/api/recommendation")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(RecommendationResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.warn("Le microservice IA est indisponible ou a renvoyé une erreur : {}", e.getMessage());
            return Optional.empty();
        }
    }
}
