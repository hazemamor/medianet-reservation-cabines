package com.medianet.reservation.service;

import com.medianet.reservation.client.RecommendationClient;
import com.medianet.reservation.dto.RecommendationRequest;
import com.medianet.reservation.dto.RecommendationResponse;
import com.medianet.reservation.entity.Cabine;
import com.medianet.reservation.entity.Equipement;
import com.medianet.reservation.entity.enums.EtatCabine;
import com.medianet.reservation.entity.enums.StatutReservation;
import com.medianet.reservation.exception.BusinessException;
import com.medianet.reservation.repository.CabineRepository;
import com.medianet.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Orchestration de la recommandation de cabine (6.7), qui respecte la règle de gestion
 * "l'IA ne recommande que des cabines disponibles".
 *
 * <p><b>État actuel (V1) :</b> aucun historique d'usage n'existe encore pour entraîner
 * un modèle Random Forest, ce qui rendrait tout modèle de ML non fiable (pas de données
 * = pas d'apprentissage significatif, risque de surapprentissage ou de biais arbitraires).
 * Le moteur repose donc sur un <b>scoring pondéré déterministe et explicable</b> (adéquation
 * de la capacité + taux de couverture des équipements souhaités), ce qui correspond déjà
 * au format de sortie attendu (cabine, score, justification) décrit en 6.7.2.</p>
 *
 * <p><b>Évolution prévue :</b> une fois plusieurs mois de réservations réelles collectés
 * (features : capacité demandée, équipements, créneaux, taux d'acceptation/annulation...),
 * le microservice FastAPI + Random Forest (voir architecture logique/physique) pourra être
 * entraîné et activé via {@code app.ia.activer-microservice=true} sans changer le contrat
 * d'API côté frontend Angular, {@link RecommendationClient} étant déjà l'interface d'appel.</p>
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CabineRepository cabineRepository;
    private final ReservationRepository reservationRepository;
    private final RecommendationClient recommendationClient;

    @org.springframework.beans.factory.annotation.Value("${app.ia.activer-microservice:false}")
    private boolean microserviceActive;

    public RecommendationResponse recommander(Integer nombreParticipants,
                                               List<String> equipementsSouhaites,
                                               LocalDateTime dateDebut,
                                               LocalDateTime dateFin) {

        List<Cabine> candidates = cabineRepository.findByEtat(EtatCabine.DISPONIBLE).stream()
                .filter(c -> c.getCapacite() >= nombreParticipants)
                .filter(c -> dateDebut == null || dateFin == null
                        || !reservationRepository.existsConflict(c.getId(), dateDebut, dateFin,
                                StatutReservation.ANNULEE))
                .toList();

        if (candidates.isEmpty()) {
            throw new BusinessException("Aucune cabine disponible ne correspond aux critères demandés");
        }

        RecommendationRequest request = new RecommendationRequest(
                nombreParticipants,
                equipementsSouhaites,
                dateDebut != null ? dateDebut.toString() : null,
                dateFin != null ? dateFin.toString() : null,
                candidates.stream().map(c -> new RecommendationRequest.CabineCandidate(
                        c.getId(), c.getNom(), c.getCapacite(),
                        c.getEquipements().stream().map(Equipement::getNom).toList()
                )).toList()
        );

        if (microserviceActive) {
            return recommendationClient.obtenirRecommandation(request)
                    .orElseGet(() -> scoringPondereV1(candidates, nombreParticipants, equipementsSouhaites));
        }

        return scoringPondereV1(candidates, nombreParticipants, equipementsSouhaites);
    }

    /**
     * Moteur de scoring pondéré V1 (voir Javadoc de la classe) : score basé sur
     * l'adéquation de la capacité et le taux de couverture des équipements souhaités.
     */
    private RecommendationResponse scoringPondereV1(List<Cabine> candidates,
                                                                   Integer nombreParticipants,
                                                                   List<String> equipementsSouhaites) {
        Set<String> souhaites = equipementsSouhaites == null ? Set.of()
                : Set.copyOf(equipementsSouhaites.stream().map(String::toLowerCase).toList());

        Cabine meilleure = candidates.stream()
                .max(Comparator.comparingDouble(c -> scoreLocal(c, nombreParticipants, souhaites)))
                .orElseThrow();

        double score = scoreLocal(meilleure, nombreParticipants, souhaites);

        String justification = "Capacité adaptée (%d places), %d/%d équipements souhaités disponibles, cabine libre sur le créneau demandé."
                .formatted(meilleure.getCapacite(), nbEquipementsCorrespondants(meilleure, souhaites), souhaites.size());

        return new RecommendationResponse(meilleure.getId(), meilleure.getNom(), Math.round(score * 100.0) / 100.0, justification);
    }

    private double scoreLocal(Cabine cabine, int nombreParticipants, Set<String> souhaites) {
        double scoreCapacite = 1.0 - Math.min(1.0, (double) Math.abs(cabine.getCapacite() - nombreParticipants) / Math.max(1, cabine.getCapacite()));
        double scoreEquipements = souhaites.isEmpty() ? 1.0 : (double) nbEquipementsCorrespondants(cabine, souhaites) / souhaites.size();
        return (0.5 * scoreCapacite + 0.5 * scoreEquipements);
    }

    private long nbEquipementsCorrespondants(Cabine cabine, Set<String> souhaites) {
        return cabine.getEquipements().stream()
                .map(e -> e.getNom().toLowerCase())
                .filter(souhaites::contains)
                .count();
    }
}
