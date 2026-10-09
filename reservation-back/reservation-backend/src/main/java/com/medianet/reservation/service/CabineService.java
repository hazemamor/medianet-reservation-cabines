package com.medianet.reservation.service;

import com.medianet.reservation.dto.CabineDto;
import com.medianet.reservation.dto.RechercheCabineRequest;
import com.medianet.reservation.entity.Cabine;
import com.medianet.reservation.entity.Equipement;
import com.medianet.reservation.entity.enums.EtatCabine;
import com.medianet.reservation.entity.enums.StatutReservation;
import com.medianet.reservation.exception.BusinessException;
import com.medianet.reservation.exception.ResourceNotFoundException;
import com.medianet.reservation.repository.CabineRepository;
import com.medianet.reservation.repository.EquipementRepository;
import com.medianet.reservation.repository.ReservationRepository;
import com.medianet.reservation.repository.spec.CabineSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CabineService {

    private final CabineRepository cabineRepository;
    private final EquipementRepository equipementRepository;
    private final ReservationRepository reservationRepository;

    public List<CabineDto> lister() {
        return cabineRepository.findAll().stream().map(this::toDto).toList();
    }

    public CabineDto obtenir(Long id) {
        return toDto(getEntity(id));
    }

    @Transactional
    public CabineDto creer(CabineDto dto) {
        cabineRepository.findByNumero(dto.numero()).ifPresent(c -> {
            throw new BusinessException("Une cabine avec le numéro " + dto.numero() + " existe déjà");
        });
        validerHoraires(dto);

        Cabine cabine = Cabine.builder()
                .nom(dto.nom())
                .numero(dto.numero())
                .capacite(dto.capacite())
                .description(dto.description())
                .localisation(dto.localisation())
                .etat(dto.etat() != null ? dto.etat() : EtatCabine.DISPONIBLE)
                .equipements(resoudreEquipements(dto.equipements()))
                .heureDisponibiliteDebut(dto.heureDisponibiliteDebut() != null ? dto.heureDisponibiliteDebut() : LocalTime.of(9, 0))
                .heureDisponibiliteFin(dto.heureDisponibiliteFin() != null ? dto.heureDisponibiliteFin() : LocalTime.of(18, 0))
                .photoUrl(dto.photoUrl())
                .build();

        return toDto(cabineRepository.save(cabine));
    }

    @Transactional
    public CabineDto modifier(Long id, CabineDto dto) {
        Cabine cabine = getEntity(id);
        validerHoraires(dto);
        cabine.setNom(dto.nom());
        cabine.setCapacite(dto.capacite());
        cabine.setDescription(dto.description());
        cabine.setLocalisation(dto.localisation());
        cabine.setPhotoUrl(dto.photoUrl());
        if (dto.heureDisponibiliteDebut() != null) {
            cabine.setHeureDisponibiliteDebut(dto.heureDisponibiliteDebut());
        }
        if (dto.heureDisponibiliteFin() != null) {
            cabine.setHeureDisponibiliteFin(dto.heureDisponibiliteFin());
        }
        if (dto.equipements() != null) {
            cabine.setEquipements(resoudreEquipements(dto.equipements()));
        }
        return toDto(cabineRepository.save(cabine));
    }

    @Transactional
    public void supprimer(Long id) {
        if (!cabineRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cabine introuvable : " + id);
        }
        cabineRepository.deleteById(id);
    }

    @Transactional
    public CabineDto mettreEnMaintenance(Long id) {
        Cabine cabine = getEntity(id);
        cabine.setEtat(EtatCabine.MAINTENANCE);
        return toDto(cabineRepository.save(cabine));
    }

    @Transactional
    public CabineDto remettreDisponible(Long id) {
        Cabine cabine = getEntity(id);
        cabine.setEtat(EtatCabine.DISPONIBLE);
        return toDto(cabineRepository.save(cabine));
    }

    /**
     * Recherche multicritère (6.4) : nombre de personnes / capacité, équipements,
     * disponibilité sur un créneau date/heure donné.
     */
    public List<CabineDto> rechercher(RechercheCabineRequest criteres) {
        Integer capaciteMin = criteres.capaciteMin() != null ? criteres.capaciteMin() : criteres.nombrePersonnes();

        Specification<Cabine> spec = Specification
                .where(CabineSpecifications.etatEgal(EtatCabine.DISPONIBLE))
                .and(CabineSpecifications.capaciteAuMoins(capaciteMin));

        List<Cabine> cabines = cabineRepository.findAll(spec);

        return cabines.stream()
                .filter(c -> possedeTousLesEquipements(c, criteres.equipements()))
                .filter(c -> estDansPlageHoraireCabine(c, criteres))
                .filter(c -> estDisponibleSurCreneau(c, criteres))
                .map(this::toDto)
                .toList();
    }

    private void validerHoraires(CabineDto dto) {
        if (dto.heureDisponibiliteDebut() != null && dto.heureDisponibiliteFin() != null
                && !dto.heureDisponibiliteDebut().isBefore(dto.heureDisponibiliteFin())) {
            throw new BusinessException("L'heure de début de disponibilité doit être avant l'heure de fin");
        }
    }

    /** Le créneau demandé doit être entièrement compris dans la plage horaire de disponibilité de la cabine. */
    private boolean estDansPlageHoraireCabine(Cabine cabine, RechercheCabineRequest criteres) {
        if (criteres.dateDebut() == null || criteres.dateFin() == null) {
            return true;
        }
        return !criteres.dateDebut().toLocalTime().isBefore(cabine.getHeureDisponibiliteDebut())
                && !criteres.dateFin().toLocalTime().isAfter(cabine.getHeureDisponibiliteFin());
    }

    private boolean possedeTousLesEquipements(Cabine cabine, List<String> equipementsDemandes) {
        if (equipementsDemandes == null || equipementsDemandes.isEmpty()) {
            return true;
        }
        Set<String> nomsCabine = new HashSet<>();
        cabine.getEquipements().forEach(e -> nomsCabine.add(e.getNom().toLowerCase()));
        return equipementsDemandes.stream()
                .map(String::toLowerCase)
                .allMatch(nomsCabine::contains);
    }

    private boolean estDisponibleSurCreneau(Cabine cabine, RechercheCabineRequest criteres) {
        if (criteres.dateDebut() == null || criteres.dateFin() == null) {
            return true;
        }
        return !reservationRepository.existsConflict(cabine.getId(), criteres.dateDebut(), criteres.dateFin(),
                StatutReservation.ANNULEE);
    }

    private Set<Equipement> resoudreEquipements(List<String> noms) {
        if (noms == null || noms.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(equipementRepository.findByNomInIgnoreCase(noms));
    }

    private Cabine getEntity(Long id) {
        return cabineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cabine introuvable : " + id));
    }

    private CabineDto toDto(Cabine c) {
        return new CabineDto(
                c.getId(), c.getNom(), c.getNumero(), c.getCapacite(),
                c.getDescription(), c.getLocalisation(), c.getEtat(),
                c.getEquipements().stream().map(Equipement::getNom).toList(),
                c.getHeureDisponibiliteDebut(), c.getHeureDisponibiliteFin(), c.getPhotoUrl()
        );
    }
}
