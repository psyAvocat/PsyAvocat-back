package com.psyavocat.dto.orientation;

import java.util.List;

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
    private String code;
    private String libelle;
    private String valeur;
    private Integer poids;
    private String questionId;
    private List<PonderationDTO> ponderations;
}
