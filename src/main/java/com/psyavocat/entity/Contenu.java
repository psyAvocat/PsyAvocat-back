package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contenus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contenu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String titre;

    private String description;

    @Column(columnDefinition = "LONGTEXT")
    private String contenu;

    private String type;

    private LocalDateTime datePublication;

    private Boolean actif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    /** Date de dernière modification par l'auteur. */
    private LocalDateTime dateModification;

    /** Catégorie : spécialité du référentiel, du même univers que l'auteur. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialite_id")
    private Specialite specialite;

    /** Image d'illustration (bucket R2 psyavocat-media). */
    private String imageObjectKey;

    /** Nombre de consultations du détail par des clients (sert au tri « Populaires »). */
    private Long nombreVues = 0L;
}
