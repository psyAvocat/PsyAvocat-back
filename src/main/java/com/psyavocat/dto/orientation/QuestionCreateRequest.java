package com.psyavocat.dto.orientation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête de création/modification d'une question depuis l'espace Admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreateRequest {

    @NotBlank(message = "Le texte de la question est obligatoire")
    private String texte;

    private String code;

    private Integer ordre;

    private Boolean obligatoire;

    private String contexte;

    private String typeReponse;

    @NotBlank(message = "L'identifiant du questionnaire est obligatoire")
    private String questionnaireId;

    public static QuestionCreateRequestBuilder builder() {
        return new QuestionCreateRequestBuilder();
    }

    public static class QuestionCreateRequestBuilder {
        private String texte;
        private String code;
        private Integer ordre;
        private Boolean obligatoire;
        private String contexte;
        private String typeReponse;
        private String questionnaireId;

        public QuestionCreateRequestBuilder texte(String texte) {
            this.texte = texte;
            return this;
        }

        public QuestionCreateRequestBuilder code(String code) {
            this.code = code;
            return this;
        }

        public QuestionCreateRequestBuilder ordre(Integer ordre) {
            this.ordre = ordre;
            return this;
        }

        public QuestionCreateRequestBuilder obligatoire(Boolean obligatoire) {
            this.obligatoire = obligatoire;
            return this;
        }

        public QuestionCreateRequestBuilder contexte(String contexte) {
            this.contexte = contexte;
            return this;
        }

        public QuestionCreateRequestBuilder typeReponse(String typeReponse) {
            this.typeReponse = typeReponse;
            return this;
        }

        public QuestionCreateRequestBuilder questionnaireId(String questionnaireId) {
            this.questionnaireId = questionnaireId;
            return this;
        }

        public QuestionCreateRequest build() {
            QuestionCreateRequest obj = new QuestionCreateRequest();
            obj.texte = this.texte;
            obj.code = this.code;
            obj.ordre = this.ordre;
            obj.obligatoire = this.obligatoire;
            obj.contexte = this.contexte;
            obj.typeReponse = this.typeReponse;
            obj.questionnaireId = this.questionnaireId;
            return obj;
        }
    }
}
