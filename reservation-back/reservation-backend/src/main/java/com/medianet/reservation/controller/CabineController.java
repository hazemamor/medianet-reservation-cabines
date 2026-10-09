package com.medianet.reservation.controller;

import com.medianet.reservation.dto.CabineDto;
import com.medianet.reservation.dto.CreneauOccupeDto;
import com.medianet.reservation.dto.RechercheCabineRequest;
import com.medianet.reservation.service.CabineService;
import com.medianet.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cabines")
@RequiredArgsConstructor
public class CabineController {

    private final CabineService cabineService;
    private final ReservationService reservationService;

    @GetMapping
    public List<CabineDto> lister() {
        return cabineService.lister();
    }

    @GetMapping("/{id}")
    public CabineDto obtenir(@PathVariable Long id) {
        return cabineService.obtenir(id);
    }

    @PostMapping("/recherche")
    public List<CabineDto> rechercher(@RequestBody RechercheCabineRequest criteres) {
        return cabineService.rechercher(criteres);
    }

    /**
     * Creneaux deja reserves de la cabine pour le jour demande (sans identite des employes) :
     * affiches sur la page de reservation pour aider l'employe a choisir un creneau libre.
     */
    @GetMapping("/{id}/creneaux-occupes")
    public List<CreneauOccupeDto> creneauxOccupes(@PathVariable Long id,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.creneauxOccupes(id, date);
    }
}
