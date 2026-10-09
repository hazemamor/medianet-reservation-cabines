package com.medianet.reservation.controller;

import com.medianet.reservation.dto.CabineDto;
import com.medianet.reservation.service.CabineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cabines")
@RequiredArgsConstructor
public class AdminCabineController {

    private final CabineService cabineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CabineDto creer(@Valid @RequestBody CabineDto dto) {
        return cabineService.creer(dto);
    }

    @PutMapping("/{id}")
    public CabineDto modifier(@PathVariable Long id, @Valid @RequestBody CabineDto dto) {
        return cabineService.modifier(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        cabineService.supprimer(id);
    }

    @PatchMapping("/{id}/maintenance")
    public CabineDto mettreEnMaintenance(@PathVariable Long id) {
        return cabineService.mettreEnMaintenance(id);
    }

    @PatchMapping("/{id}/disponible")
    public CabineDto remettreDisponible(@PathVariable Long id) {
        return cabineService.remettreDisponible(id);
    }
}
