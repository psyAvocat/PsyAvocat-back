package com.psyavocat.dto.dossier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PieceJointeDTO {
    private String id;
    private String nom;
    private String url;
    private LocalDate dateAjout;
}
