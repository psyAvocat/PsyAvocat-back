package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories_besoin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategorieBesoin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    private String nom;

    private String description;

    private String typeProfessionnel;

    private Boolean actif;

    @ManyToMany(mappedBy = "categoriesBesoin")
    private List<Specialite> specialites = new ArrayList<>();
}
