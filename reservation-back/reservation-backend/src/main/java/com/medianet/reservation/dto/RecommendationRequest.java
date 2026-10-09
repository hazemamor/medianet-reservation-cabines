package com.medianet.reservation.dto;

import java.util.List;

/**
 * Payload envoyé au microservice FastAPI (Random Forest) pour obtenir
 * une recommandation de cabine (section 6.7 du cahier des charges).
 */
public record RecommendationRequest(
        Integer nombreParticipants,
        List<String> equipementsSouhaites,
        String dateDebut,
        String dateFin,
        List<CabineCandidate> cabinesDisponibles
) {
    public record CabineCandidate(
            Long id,
            String nom,
            Integer capacite,
            List<String> equipements
    ) {}
}
