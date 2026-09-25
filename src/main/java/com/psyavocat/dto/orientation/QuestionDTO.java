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
    private String texte;
    private Integer ordre;
    private Boolean obligatoire;
    private List<ReponseDTO> reponses;
}
