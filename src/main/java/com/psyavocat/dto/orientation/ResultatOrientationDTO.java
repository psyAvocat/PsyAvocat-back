package com.psyavocat.dto.orientation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String categorieBesoinNom;
    private String categorieBesoinDescription;
}
