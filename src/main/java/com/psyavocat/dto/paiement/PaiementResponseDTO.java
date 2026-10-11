package com.psyavocat.dto.paiement;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Paiement (simulé en V1) du client connecté : acompte d'un rendez-vous.
 * {@code simulation = true} : aucune transaction financière réelle n'a eu lieu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaiementResponseDTO {
    private String id;
    private BigDecimal montant;
    private String devise;
    private LocalDateTime datePaiement;
    /** PAYE (acompte simulé validé) ou ANNULE (rendez-vous annulé). */
    private String statut;
    private String methode;
    private boolean simulation;
    private String rendezVousId;
    private LocalDateTime rendezVousDate;
    private String statutRendezVous;
    private BigDecimal montantTotalRendezVous;
    private String professionnelNom;
    private String professionnelPrenom;
    /** AVOCAT ou PSYCHOLOGUE : univers du paiement. */
    private String typeProfessionnel;
}
