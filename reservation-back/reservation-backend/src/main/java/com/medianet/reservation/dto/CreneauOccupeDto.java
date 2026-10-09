package com.medianet.reservation.dto;

import java.time.LocalTime;

/**
 * Représente un créneau déjà réservé sur une cabine, pour un jour donné.
 * Volontairement anonyme (pas de nom/email d'employé) : c'est exposé aux employés
 * eux-mêmes pour les aider à choisir un créneau libre, pas seulement à l'admin.
 */
public record CreneauOccupeDto(
        LocalTime heureDebut,
        LocalTime heureFin
) {}
