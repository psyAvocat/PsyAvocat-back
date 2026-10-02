package com.psyavocat.dto.orientation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête de création/modification d'un questionnaire depuis l'espace Admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireCreateRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String code;

    @NotBlank(message = "Le type est obligatoire (PSYCHOLOGIQUE ou JURIDIQUE)")
    private String type;

    @NotNull(message = "L'état actif est obligatoire")
    private Boolean actif;

    public static QuestionnaireCreateRequestBuilder builder() {
        return new QuestionnaireCreateRequestBuilder();
    }

    public static class QuestionnaireCreateRequestBuilder {
        private String titre;
        private String code;
        private String type;
        private Boolean actif;

        public QuestionnaireCreateRequestBuilder titre(String titre) {
            this.titre = titre;
            return this;
        }

        public QuestionnaireCreateRequestBuilder code(String code) {
            this.code = code;
            return this;
        }

        public QuestionnaireCreateRequestBuilder type(String type) {
            this.type = type;
            return this;
        }

        public QuestionnaireCreateRequestBuilder actif(Boolean actif) {
            this.actif = actif;
            return this;
        }

        public QuestionnaireCreateRequest build() {
            QuestionnaireCreateRequest obj = new QuestionnaireCreateRequest();
            obj.titre = this.titre;
            obj.code = this.code;
            obj.type = this.type;
            obj.actif = this.actif;
            return obj;
        }
    }
}
