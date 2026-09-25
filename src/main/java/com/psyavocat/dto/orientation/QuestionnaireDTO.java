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
public class QuestionnaireDTO {
    private String id;
    private String titre;
    private String type;
    private Boolean actif;
    private List<QuestionDTO> questions;
}
