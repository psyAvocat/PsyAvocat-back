package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Représente le score obtenu pour une catégorie de besoin dans un résultat d'orientation.
 * Permet de conserver le classement complet (toutes catégories avec score + rang).
 */
@Entity
@Table(name = "resultat_orientation_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultatOrientationCategorie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resultat_orientation_id", nullable = false)
    private ResultatOrientation resultatOrientation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_besoin_id", nullable = false)
    private CategorieBesoin categorieBesoin;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Integer rang;
}
