package com.psyavocat.dto.referentiel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialiteDTO {
    private String id;
    private String nom;
    private String description;
    private String domaineId;
    private String domaineNom;
    private List<String> categorieIds;
    private List<CategorieBesoinDTO> categories;

    public SpecialiteDTO(String id, String nom, String description) {
        this.id = id;
        this.nom = nom;
        this.description = description;
    }
}
