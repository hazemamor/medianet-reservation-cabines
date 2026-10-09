package com.medianet.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hash BCrypt du code OTP (jamais stocké en clair), à l'image d'un mot de passe.
     * Empêche toute lecture du code en cas de fuite/dump de la base de données.
     */
    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Builder.Default
    @Column(nullable = false)
    private Boolean utilise = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer tentatives = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
}
