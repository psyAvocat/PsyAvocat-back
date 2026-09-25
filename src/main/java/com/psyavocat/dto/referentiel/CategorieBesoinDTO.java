package com.psyavocat.dto.referentiel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorieBesoinDTO {
    private String id;
    private String nom;
    private String description;
    private String typeProfessionnel;
    private Boolean actif;
}
