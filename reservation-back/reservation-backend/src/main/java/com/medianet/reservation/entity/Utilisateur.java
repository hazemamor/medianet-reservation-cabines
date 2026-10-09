package com.medianet.reservation.entity;

import com.medianet.reservation.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateurs", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Si non nulle et dans le futur, l'utilisateur est temporairement bloqué
     * suite à trop de tentatives OTP erronées (règle de blocage, 6.1.2).
     */
    private LocalDateTime bloqueJusqua;

    @Builder.Default
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Otp> otps = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();
}
