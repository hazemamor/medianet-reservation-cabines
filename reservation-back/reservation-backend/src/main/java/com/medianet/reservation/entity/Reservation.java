package com.medianet.reservation.entity;

import com.medianet.reservation.entity.enums.StatutReservation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dateReservation = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean presenceConfirmee = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabine_id", nullable = false)
    private Cabine cabine;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "recommandation_id")
    private Recommandation recommandation;
}
