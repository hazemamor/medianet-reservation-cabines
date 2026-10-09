package com.medianet.reservation.entity;

import com.medianet.reservation.entity.enums.EtatCabine;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cabines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cabine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(nullable = false)
    private Integer capacite;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String localisation;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EtatCabine etat = EtatCabine.DISPONIBLE;

    /** Heure a partir de laquelle la cabine peut etre reservee dans la journee (ex: 09:00). */
    @Builder.Default
    @Column(nullable = false)
    private LocalTime heureDisponibiliteDebut = LocalTime.of(9, 0);

    /** Heure limite de reservation dans la journee (ex: 18:00). */
    @Builder.Default
    @Column(nullable = false)
    private LocalTime heureDisponibiliteFin = LocalTime.of(18, 0);

    /** URL de la photo illustrant la cabine (affichee dans la liste et le detail cote employe). */
    @Column(length = 500)
    private String photoUrl;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "cabine_equipement",
            joinColumns = @JoinColumn(name = "cabine_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id")
    )
    private Set<Equipement> equipements = new HashSet<>();
}
