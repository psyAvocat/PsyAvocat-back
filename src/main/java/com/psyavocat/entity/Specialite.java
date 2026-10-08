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

    @Column(name = "type_professionnel")
    private String typeProfessionnel; // AVOCAT, PSYCHOLOGUE

    public String resolveTypeProfessionnel() {
        if (this.typeProfessionnel != null && !this.typeProfessionnel.isBlank()) {
            return this.typeProfessionnel.trim().toUpperCase();
        }
        if (this.categoriesBesoin != null && !this.categoriesBesoin.isEmpty()) {
            for (CategorieBesoin cat : this.categoriesBesoin) {
                if (cat.getTypeProfessionnel() != null && !cat.getTypeProfessionnel().isBlank()) {
                    return cat.getTypeProfessionnel().trim().toUpperCase();
                }
            }
        }
        if (this.domaine != null && this.domaine.getNom() != null) {
            String d = this.domaine.getNom().toLowerCase();
            if (d.contains("droit") || d.contains("juridique") || d.contains("avocat") || d.contains("travail") || d.contains("affaires") || d.contains("pénal")) {
                return "AVOCAT";
            }
            if (d.contains("psy") || d.contains("santé") || d.contains("mental") || d.contains("thérapie") || d.contains("clinique")) {
                return "PSYCHOLOGUE";
            }
        }
        return null;
    }

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
