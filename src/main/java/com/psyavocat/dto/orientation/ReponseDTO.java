package com.psyavocat.dto.orientation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReponseDTO {
    private String id;
    private String libelle;
    private String valeur;
    private Integer poids;
}
