package com.psyavocat.dto.orientation;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultatOrientationDTO {

    private String id;
    private LocalDateTime dateEvaluation;
    private Double score;
    private String questionnaireId;
    private String questionnaireTitre;
    private String categorieBesoinId;
    private String categorieBesoinCode;
    private String categorieBesoinNom;
    private String categorieBesoinDescription;
    private String statutDepartage;
    private String specialiteId;
    private String specialiteNom;
    private String specialiteDescription;
    private String domaineNom;
    private List<ResultatOrientationCategorieDTO> scoresParCategorie;
    private List<ProfessionnelResponseDTO> professionnelsRecommandes;
}
