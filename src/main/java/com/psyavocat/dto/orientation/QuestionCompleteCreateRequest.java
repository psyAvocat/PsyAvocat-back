package com.psyavocat.dto.orientation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Requête atomique de création ou modification complète d'une question
 * avec toutes ses réponses et leurs pondérations respectives.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCompleteCreateRequest {

    @NotBlank(message = "L'identifiant du questionnaire est obligatoire")
    private String questionnaireId;

    @NotBlank(message = "Le texte de la question est obligatoire")
    private String texte;

    private String code;

    private Integer ordre;

    private Boolean obligatoire;

    private String contexte;

    private String typeReponse; // CHOIX_UNIQUE, CHOIX_MULTIPLE, OUI_NON

    @Valid
    @NotEmpty(message = "Une question doit comporter au moins une réponse")
    @Builder.Default
    private List<ReponseCompleteItemRequest> reponses = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReponseCompleteItemRequest {
        private String id; // Optionnel (pour les modifications)

        @NotBlank(message = "Le libellé de la réponse est obligatoire")
        private String libelle;

        private String code;

        private String valeur;

        @Valid
        @Builder.Default
        private List<PonderationItemRequest> ponderations = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PonderationItemRequest {
        private String id; // Optionnel (pour les modifications)

        private String categorieBesoinId;

        private String specialiteId;

        @NotNull(message = "Le poids de la pondération est obligatoire")
        @Min(value = 0, message = "Le poids ne peut pas être négatif")
        private Integer poids;
    }
}
