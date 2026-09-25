package com.psyavocat.dto.rendezvous;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RendezVousResponseDTO {

    private String id;
    private LocalDateTime dateHeure;
    private String statut;
    private String mode;

    private String patientId;
    private String patientNom;
    private String patientPrenom;

    private String professionnelId;
    private String professionnelNom;
    private String professionnelPrenom;
    private String typeProfessionnel; // AVOCAT, PSYCHOLOGUE

    private BigDecimal montantTotal;
    private BigDecimal montantAcompte; // 20%
    private String statutPaiement;
}
