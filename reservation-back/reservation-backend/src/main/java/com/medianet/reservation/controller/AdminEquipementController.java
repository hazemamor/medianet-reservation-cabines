package com.medianet.reservation.controller;

import com.medianet.reservation.dto.EquipementDto;
import com.medianet.reservation.service.EquipementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/equipements")
@RequiredArgsConstructor
public class AdminEquipementController {

    private final EquipementService equipementService;

    @GetMapping
    public List<EquipementDto> lister() {
        return equipementService.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipementDto creer(@Valid @RequestBody EquipementDto dto) {
        return equipementService.creer(dto);
    }

    @PutMapping("/{id}")
    public EquipementDto modifier(@PathVariable Long id, @Valid @RequestBody EquipementDto dto) {
        return equipementService.modifier(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        equipementService.supprimer(id);
    }
}
