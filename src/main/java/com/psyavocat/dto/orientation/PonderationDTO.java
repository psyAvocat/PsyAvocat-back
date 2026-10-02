package com.psyavocat.dto.orientation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représentation d'une pondération d'orientation pour l'espace Admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PonderationDTO {

    private String id;
    private String reponseId;
    private String reponseLibelle;

    private String categorieBesoinId;
    private String categorieBesoinNom;

    private String specialiteId;
    private String specialiteNom;

    private Integer poids;

    public static PonderationDTOBuilder builder() {
        return new PonderationDTOBuilder();
    }

    public static class PonderationDTOBuilder {
        private String id;
        private String reponseId;
        private String reponseLibelle;
        private String categorieBesoinId;
        private String categorieBesoinNom;
        private String specialiteId;
        private String specialiteNom;
        private Integer poids;

        public PonderationDTOBuilder id(String id) {
            this.id = id;
            return this;
        }

        public PonderationDTOBuilder reponseId(String reponseId) {
            this.reponseId = reponseId;
            return this;
        }

        public PonderationDTOBuilder reponseLibelle(String reponseLibelle) {
            this.reponseLibelle = reponseLibelle;
            return this;
        }

        public PonderationDTOBuilder categorieBesoinId(String categorieBesoinId) {
            this.categorieBesoinId = categorieBesoinId;
            return this;
        }

        public PonderationDTOBuilder categorieBesoinNom(String categorieBesoinNom) {
            this.categorieBesoinNom = categorieBesoinNom;
            return this;
        }

        public PonderationDTOBuilder specialiteId(String specialiteId) {
            this.specialiteId = specialiteId;
            return this;
        }

        public PonderationDTOBuilder specialiteNom(String specialiteNom) {
            this.specialiteNom = specialiteNom;
            return this;
        }

        public PonderationDTOBuilder poids(Integer poids) {
            this.poids = poids;
            return this;
        }

        public PonderationDTO build() {
            PonderationDTO obj = new PonderationDTO();
            obj.id = this.id;
            obj.reponseId = this.reponseId;
            obj.reponseLibelle = this.reponseLibelle;
            obj.categorieBesoinId = this.categorieBesoinId;
            obj.categorieBesoinNom = this.categorieBesoinNom;
            obj.specialiteId = this.specialiteId;
            obj.specialiteNom = this.specialiteNom;
            obj.poids = this.poids;
            return obj;
        }
    }
}
