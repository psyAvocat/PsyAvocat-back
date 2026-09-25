package com.psyavocat.dto.dossier;

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
public class SoumissionDossierResponseDTO {

    private String id;
    private String dossierId;
    private String dossierTitre;
    private String dossierDescription;
    private String justiciableId;
    private String justiciableNom;
    private String justiciablePrenom;
    private String avocatId;
    private String avocatNom;
    private String avocatPrenom;
    private LocalDateTime dateSoumission;
    private String statut; // EN_ATTENTE, ACCEPTEE, REFUSEE
    private String reponse;
    private BigDecimal tarifPropose;
    private LocalDateTime dateReponse;
}
