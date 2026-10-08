package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dossiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dateOuverture;

    private String statut;

    /**
     * Client propriétaire : profil client unifié ({@link Client}) ou ancien {@link Justiciable}.
     * Typé {@link Utilisateur} pour accepter les deux (voir ClientAccountMigration).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "justiciable_id")
    private Utilisateur justiciable;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PieceJointe> piecesJointes = new ArrayList<>();

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Echeance> echeances = new ArrayList<>();

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoumissionDossier> soumissions = new ArrayList<>();
}
