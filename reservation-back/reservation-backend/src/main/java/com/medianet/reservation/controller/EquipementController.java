package com.medianet.reservation.controller;

import com.medianet.reservation.dto.EquipementDto;
import com.medianet.reservation.service.EquipementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipements")
@RequiredArgsConstructor
public class EquipementController {

    private final EquipementService equipementService;

    @GetMapping
    public List<EquipementDto> lister() {
        return equipementService.lister();
    }
}
