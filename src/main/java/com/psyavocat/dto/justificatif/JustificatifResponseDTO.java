package com.psyavocat.dto.justificatif;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JustificatifResponseDTO {
    private String id;
    private String nomFichier;
    private String typeDocument;
    private String statutValidation;
    private LocalDate dateDepot;
    private String professionnelId;
    private Long tailleOctets;
    private String typeMime;
    private String urlVisualisation; // URL présignée temporaire (durée de vie limitée)

    // --- Mise à jour d'un document déjà validé (vue administrateur) ---
    /** Nom du professionnel (listes de l'administration). */
    private String professionnelNom;
    /** AVOCAT ou PSYCHOLOGUE. */
    private String typeProfessionnel;
    /** Document validé que celui-ci remplacera s'il est approuvé (null pour un premier dépôt). */
    private String remplaceNomFichier;
    private String remplaceId;

    // --- Décision de l'administration (visible par le professionnel) ---
    /** Motif communiqué par l'administration lorsque le document a été refusé. */
    private String motifRejet;
    /** Date de la dernière décision (validation ou refus). */
    private LocalDateTime dateDecision;
}
