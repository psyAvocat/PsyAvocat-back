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
public class DomaineDTO {
    private String id;
    /** PSYCHOLOGIQUE ou JURIDIQUE : les deux domaines sont fixes. */
    private String code;
    private String nom;
    private String description;
    /** AVOCAT ou PSYCHOLOGUE. */
    private String typeProfessionnel;
    private List<SpecialiteDTO> specialites;
    private Long nombreSpecialites;
}
