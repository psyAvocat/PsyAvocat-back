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
    /** Lien de consultation temporaire (URL présignée), jamais un lien public permanent. */
    private String url;
    private LocalDate dateAjout;
    private String typeMime;
    private Long tailleOctets;

    public PieceJointeDTO(String id, String nom, String url, LocalDate dateAjout) {
        this(id, nom, url, dateAjout, null, null);
    }
}
