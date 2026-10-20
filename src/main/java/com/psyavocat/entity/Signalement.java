package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signalements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Signalement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Commentaire de l'auteur : jusqu'à 1 000 caractères (CreateSignalementRequest). */
    @Column(length = 1000)
    private String contenuVise;

    private String motif;

    private String statut;

    private LocalDateTime dateSignalement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_vise_id")
    private Utilisateur utilisateurVise;
}
