package com.medianet.reservation.service;

import com.medianet.reservation.dto.EquipementDto;
import com.medianet.reservation.entity.Equipement;
import com.medianet.reservation.exception.ResourceNotFoundException;
import com.medianet.reservation.repository.EquipementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipementService {

    private final EquipementRepository equipementRepository;

    public List<EquipementDto> lister() {
        return equipementRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public EquipementDto creer(EquipementDto dto) {
        Equipement equipement = Equipement.builder()
                .nom(dto.nom())
                .description(dto.description())
                .build();
        return toDto(equipementRepository.save(equipement));
    }

    @Transactional
    public EquipementDto modifier(Long id, EquipementDto dto) {
        Equipement equipement = equipementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable : " + id));
        equipement.setNom(dto.nom());
        equipement.setDescription(dto.description());
        return toDto(equipementRepository.save(equipement));
    }

    @Transactional
    public void supprimer(Long id) {
        if (!equipementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Équipement introuvable : " + id);
        }
        equipementRepository.deleteById(id);
    }

    private EquipementDto toDto(Equipement e) {
        return new EquipementDto(e.getId(), e.getNom(), e.getDescription());
    }
}
