package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fiches_patient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FichePatient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDate dateCreation;

    /**
     * Client suivi : profil client unifié ({@link Client}) ou ancien {@link Patient}.
     * Typé {@link Utilisateur} pour accepter les deux (voir ClientAccountMigration).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Utilisateur patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psychologue_id")
    private Psychologue psychologue;

    @OneToMany(mappedBy = "fichePatient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seance> seances = new ArrayList<>();
}
