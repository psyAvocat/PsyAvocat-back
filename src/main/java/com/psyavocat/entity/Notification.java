package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    private LocalDateTime dateEnvoi;

    private String lienVisio;

    @Column(nullable = false)
    private boolean lu = false;

    /** Titre court affiché dans la liste et la notification push. */
    private String titre;

    /** Date de lecture (null tant que non lue). */
    private LocalDateTime dateLecture;

    /**
     * Univers de la ressource (AVOCAT, PSYCHOLOGUE) ou null si transverse.
     * Sert au filtrage in-app ; le push, lui, est toujours envoyé.
     */
    private String univers;

    /** Type de ressource ciblée (RENDEZ_VOUS, CONVERSATION, ARTICLE, CONSEIL...). */
    private String ressourceType;

    /** Identifiant de la ressource ciblée (lien profond). */
    private String ressourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id")
    private Utilisateur destinataire;
}
