package com.psyavocat.dto.orientation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête de création/modification d'une pondération d'orientation depuis l'espace Admin.
 * Associe une réponse à une catégorie de besoin (ou spécialité) avec un poids.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PonderationCreateRequest {

    @NotBlank(message = "L'identifiant de la réponse est obligatoire")
    private String reponseId;

    /**
     * Renseigné si la pondération cible une catégorie psychologique.
     */
    private String categorieBesoinId;

    /**
     * Renseigné si la pondération cible une spécialité juridique.
     */
    private String specialiteId;

    @NotNull(message = "Le poids est obligatoire")
    private Integer poids;

    public static PonderationCreateRequestBuilder builder() {
        return new PonderationCreateRequestBuilder();
    }

    public static class PonderationCreateRequestBuilder {
        private String reponseId;
        private String categorieBesoinId;
        private String specialiteId;
        private Integer poids;

        public PonderationCreateRequestBuilder reponseId(String reponseId) {
            this.reponseId = reponseId;
            return this;
        }

        public PonderationCreateRequestBuilder categorieBesoinId(String categorieBesoinId) {
            this.categorieBesoinId = categorieBesoinId;
            return this;
        }

        public PonderationCreateRequestBuilder specialiteId(String specialiteId) {
            this.specialiteId = specialiteId;
            return this;
        }

        public PonderationCreateRequestBuilder poids(Integer poids) {
            this.poids = poids;
            return this;
        }

        public PonderationCreateRequest build() {
            PonderationCreateRequest obj = new PonderationCreateRequest();
            obj.reponseId = this.reponseId;
            obj.categorieBesoinId = this.categorieBesoinId;
            obj.specialiteId = this.specialiteId;
            obj.poids = this.poids;
            return obj;
        }
    }
}
