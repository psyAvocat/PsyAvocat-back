package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resultats_orientation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultatOrientation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDateTime dateEvaluation;

    private Double score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur; // Accepte Patient ET Justiciable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id")
    private Questionnaire questionnaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_besoin_id")
    private CategorieBesoin categorieBesoin; // Univers Psychologue

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialite_id")
    private Specialite specialite; // Univers Avocat

    @ManyToMany
    @JoinTable(
            name = "resultat_professionnels_recommandes",
            joinColumns = @JoinColumn(name = "resultat_id"),
            inverseJoinColumns = @JoinColumn(name = "professionnel_id")
    )
    private List<Professionnel> professionnelsRecommandes = new ArrayList<>();

    /**
     * Classement complet de toutes les catégories avec leur score et rang.
     * Permet d'afficher un résultat multi-critères (Anxiété rang1, Stress rang2, etc.).
     */
    @OneToMany(mappedBy = "resultatOrientation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResultatOrientationCategorie> scoresParCategorie = new ArrayList<>();

    public Utilisateur getPatient() {
        return utilisateur;
    }

    public void setPatient(Utilisateur patient) {
        this.utilisateur = patient;
    }
}
