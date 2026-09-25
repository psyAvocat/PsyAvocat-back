package com.psyavocat.dto.orientation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoumissionQuestionnaireRequest {

    @NotBlank(message = "L'identifiant du questionnaire est obligatoire")
    private String questionnaireId;

    @NotEmpty(message = "La liste des identifiants de réponses sélectionnées ne peut pas être vide")
    private List<String> reponseIds;
}
