package com.psyavocat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Historique des décisions de l'administration (traçabilité) :
 * statut d'un professionnel, traitement d'un signalement, validation d'un justificatif.
 */
@Entity
@Table(name = "decisions_administratives", indexes = @Index(name = "idx_decision_cible", columnList = "cible_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DecisionAdministrative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** STATUT_PROFESSIONNEL, SIGNALEMENT ou JUSTIFICATIF. */
    @Column(nullable = false, length = 40)
    private String type;

    /** Identifiant de l'élément concerné (professionnel, signalement, justificatif). */
    @Column(name = "cible_id", nullable = false)
    private String cibleId;

    /** Décision prise (ex. APPROVED, SUSPENDED, TRAITE_CLASSE, VALIDE…). */
    @Column(nullable = false, length = 40)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String motif;

    /** Administrateur auteur (Firebase UID). */
    @Column(name = "administrateur_id")
    private String administrateurId;

    @Column(nullable = false)
    private LocalDateTime dateDecision;
}
