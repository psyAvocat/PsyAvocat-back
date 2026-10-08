package com.psyavocat.dto.orientation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Score d'une catégorie de besoin dans un résultat d'orientation.
 * Permet de représenter le classement complet (rang 1, rang 2, rang 3, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultatOrientationCategorieDTO {

    private String id;
    private String categorieBesoinId;
    private String categorieBesoinNom;
    private String categorieBesoinDescription;
    private Integer score;
    private Integer rang;

    public static ResultatOrientationCategorieDTOBuilder builder() {
        return new ResultatOrientationCategorieDTOBuilder();
    }

    public static class ResultatOrientationCategorieDTOBuilder {
        private String id;
        private String categorieBesoinId;
        private String categorieBesoinNom;
        private String categorieBesoinDescription;
        private Integer score;
        private Integer rang;

        public ResultatOrientationCategorieDTOBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ResultatOrientationCategorieDTOBuilder categorieBesoinId(String categorieBesoinId) {
            this.categorieBesoinId = categorieBesoinId;
            return this;
        }

        public ResultatOrientationCategorieDTOBuilder categorieBesoinNom(String categorieBesoinNom) {
            this.categorieBesoinNom = categorieBesoinNom;
            return this;
        }

        public ResultatOrientationCategorieDTOBuilder categorieBesoinDescription(String categorieBesoinDescription) {
            this.categorieBesoinDescription = categorieBesoinDescription;
            return this;
        }

        public ResultatOrientationCategorieDTOBuilder score(Integer score) {
            this.score = score;
            return this;
        }

        public ResultatOrientationCategorieDTOBuilder rang(Integer rang) {
            this.rang = rang;
            return this;
        }

        public ResultatOrientationCategorieDTO build() {
            ResultatOrientationCategorieDTO obj = new ResultatOrientationCategorieDTO();
            obj.id = this.id;
            obj.categorieBesoinId = this.categorieBesoinId;
            obj.categorieBesoinNom = this.categorieBesoinNom;
            obj.categorieBesoinDescription = this.categorieBesoinDescription;
            obj.score = this.score;
            obj.rang = this.rang;
            return obj;
        }
    }
}
