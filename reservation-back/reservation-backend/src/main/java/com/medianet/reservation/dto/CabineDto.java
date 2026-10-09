package com.medianet.reservation.dto;

import com.medianet.reservation.entity.enums.EtatCabine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;
import java.util.List;

public record CabineDto(
        Long id,
        @NotBlank String nom,
        @NotBlank String numero,
        @NotNull @Positive Integer capacite,
        String description,
        @NotBlank String localisation,
        EtatCabine etat,
        List<String> equipements,
        @NotNull LocalTime heureDisponibiliteDebut,
        @NotNull LocalTime heureDisponibiliteFin,
        String photoUrl
) {}
