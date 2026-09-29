package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ponderations_orientation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PonderationOrientation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Integer poids;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reponse_id", nullable = false)
    private Reponse reponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_besoin_id")
    private CategorieBesoin categorieBesoin; // Renseigné si impacte un besoin psychologique

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialite_id")
    private Specialite specialite; // Renseigné si impacte un domaine/spécialité juridique
}
