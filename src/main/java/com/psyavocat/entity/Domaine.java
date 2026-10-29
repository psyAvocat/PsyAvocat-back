package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "domaines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Domaine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Code du domaine fixe (PSYCHOLOGIQUE ou JURIDIQUE), voir DomainesFixes. */
    @Column(unique = true)
    private String code;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Univers du domaine (AVOCAT ou PSYCHOLOGUE) : ses spécialités appartiennent au même univers. */
    @Column(name = "type_professionnel")
    private String typeProfessionnel;

    // Aucune cascade : les domaines sont fixes, les spécialités y sont rattachées par univers.
    @OneToMany(mappedBy = "domaine")
    private List<Specialite> specialites = new ArrayList<>();
}
