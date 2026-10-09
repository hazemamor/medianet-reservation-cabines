package com.medianet.reservation.controller;

import com.medianet.reservation.dto.RecommendationResponse;
import com.medianet.reservation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/recommandations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public RecommendationResponse recommander(
            @RequestParam Integer nombreParticipants,
            @RequestParam(required = false) List<String> equipements,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin
    ) {
        return recommendationService.recommander(nombreParticipants, equipements, dateDebut, dateFin);
    }
}
