package com.medianet.reservation.controller;

import com.medianet.reservation.dto.ReservationRequest;
import com.medianet.reservation.dto.ReservationResponse;
import com.medianet.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserver(Authentication authentication, @Valid @RequestBody ReservationRequest request) {
        return reservationService.reserver(authentication.getName(), request);
    }

    @PatchMapping("/{id}/confirmer-presence")
    public ReservationResponse confirmerPresence(Authentication authentication, @PathVariable Long id) {
        return reservationService.confirmerPresence(authentication.getName(), id);
    }

    @PatchMapping("/{id}/annuler")
    public ReservationResponse annuler(Authentication authentication, @PathVariable Long id) {
        return reservationService.annuler(authentication.getName(), id);
    }

    @GetMapping("/historique")
    public List<ReservationResponse> historique(Authentication authentication) {
        return reservationService.historique(authentication.getName());
    }
}
