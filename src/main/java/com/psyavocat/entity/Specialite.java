package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "specialites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Specialite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Univers de la spécialité (AVOCAT ou PSYCHOLOGUE) : donnée enregistrée par l'administration. */
    @Column(name = "type_professionnel")
    private String typeProfessionnel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domaine_id")
    private Domaine domaine;

    @ManyToMany(mappedBy = "specialites")
    private List<Professionnel> professionnels = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "specialite_categories_besoin",
            joinColumns = @JoinColumn(name = "specialite_id"),
            inverseJoinColumns = @JoinColumn(name = "categorie_besoin_id")
    )
    private List<CategorieBesoin> categoriesBesoin = new ArrayList<>();
}
