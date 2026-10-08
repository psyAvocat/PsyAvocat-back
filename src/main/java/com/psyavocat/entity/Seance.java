package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDateTime date;

    private String statut;

    @Column(name = "rendez_vous_id")
    private String rendezVousId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiche_patient_id")
    private FichePatient fichePatient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psychologue_id")
    private Psychologue psychologue;

    @OneToMany(mappedBy = "seance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteSeance> notes = new ArrayList<>();
}
