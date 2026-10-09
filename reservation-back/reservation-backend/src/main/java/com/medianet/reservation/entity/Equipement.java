package com.medianet.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    private String description;
}
