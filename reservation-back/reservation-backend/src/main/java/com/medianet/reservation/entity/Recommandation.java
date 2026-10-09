package com.medianet.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recommandations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommandation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double score;

    @Column(length = 2000)
    private String justification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabine_id", nullable = false)
    private Cabine cabine;
}
