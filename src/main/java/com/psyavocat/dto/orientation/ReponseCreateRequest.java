package com.psyavocat.dto.orientation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête de création/modification d'une réponse depuis l'espace Admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReponseCreateRequest {

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    private String code;

    private String valeur;

    @NotBlank(message = "L'identifiant de la question est obligatoire")
    private String questionId;

    public static ReponseCreateRequestBuilder builder() {
        return new ReponseCreateRequestBuilder();
    }

    public static class ReponseCreateRequestBuilder {
        private String libelle;
        private String code;
        private String valeur;
        private String questionId;

        public ReponseCreateRequestBuilder libelle(String libelle) {
            this.libelle = libelle;
            return this;
        }

        public ReponseCreateRequestBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ReponseCreateRequestBuilder valeur(String valeur) {
            this.valeur = valeur;
            return this;
        }

        public ReponseCreateRequestBuilder questionId(String questionId) {
            this.questionId = questionId;
            return this;
        }

        public ReponseCreateRequest build() {
            ReponseCreateRequest obj = new ReponseCreateRequest();
            obj.libelle = this.libelle;
            obj.code = this.code;
            obj.valeur = this.valeur;
            obj.questionId = this.questionId;
            return obj;
        }
    }
}
