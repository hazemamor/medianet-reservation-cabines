package com.medianet.reservation.repository.spec;

import com.medianet.reservation.entity.Cabine;
import com.medianet.reservation.entity.Equipement;
import com.medianet.reservation.entity.enums.EtatCabine;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Traduit les critères de recherche multicritère (6.4 du cahier des charges) en Specification JPA.
 */
public class CabineSpecifications {

    private CabineSpecifications() {
    }

    public static Specification<Cabine> capaciteAuMoins(Integer capaciteMin) {
        return (root, query, cb) -> capaciteMin == null ? null
                : cb.greaterThanOrEqualTo(root.get("capacite"), capaciteMin);
    }

    public static Specification<Cabine> etatEgal(EtatCabine etat) {
        return (root, query, cb) -> etat == null ? null : cb.equal(root.get("etat"), etat);
    }

    public static Specification<Cabine> possedeEquipements(List<String> equipements) {
        return (root, query, cb) -> {
            if (equipements == null || equipements.isEmpty()) {
                return null;
            }
            query.distinct(true);
            Join<Cabine, Equipement> join = root.join("equipements");
            return join.get("nom").in(equipements);
        };
    }
}
