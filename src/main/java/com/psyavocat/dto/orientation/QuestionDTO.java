package com.psyavocat.dto.orientation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private String id;
    private String code;
    private String texte;
    private Integer ordre;
    private Boolean obligatoire;
    private String contexte;
    private String typeReponse;
    private List<ReponseDTO> reponses;
}
