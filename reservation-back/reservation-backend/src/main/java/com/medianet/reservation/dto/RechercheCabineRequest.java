package com.medianet.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Critères de recherche multicritère (section 6.4 du cahier des charges) :
 * nombre de personnes, capacité, équipements souhaités, disponibilité, date, heure.
 */
public record RechercheCabineRequest(
        Integer nombrePersonnes,
        Integer capaciteMin,
        List<String> equipements,
        LocalDateTime dateDebut,
        LocalDateTime dateFin
) {}
