package com.medianet.reservation.controller;

import com.medianet.reservation.dto.ReservationAdminCreationRequest;
import com.medianet.reservation.dto.ReservationAdminResponse;
import com.medianet.reservation.dto.UtilisateurResumeDto;
import com.medianet.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public List<ReservationAdminResponse> lister() {
        return reservationService.listerToutesAdmin();
    }

    /** Liste des employes, utilisee pour peupler le selecteur lors d'une reservation manuelle. */
    @GetMapping("/employes")
    public List<UtilisateurResumeDto> listerEmployes() {
        return reservationService.listerEmployes();
    }

    /**
     * Cree une reservation au nom d'un employe (ex: depuis la grille de disponibilite,
     * en cliquant sur une case libre). Aucune verification administrateur supplementaire
     * n'est necessaire au-dela du role, deja controle par SecurityConfig sur /api/admin/**.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationAdminResponse reserverPourEmploye(@Valid @RequestBody ReservationAdminCreationRequest request) {
        return reservationService.reserverPourEmploye(request);
    }

    @PatchMapping("/{id}/annuler")
    public ReservationAdminResponse annuler(@PathVariable Long id) {
        return reservationService.annulerParAdmin(id);
    }
}
