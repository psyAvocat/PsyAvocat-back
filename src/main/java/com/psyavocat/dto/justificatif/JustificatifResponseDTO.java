package com.psyavocat.dto.justificatif;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JustificatifResponseDTO {
    private String id;
    private String nomFichier;
    private String typeDocument;
    private String statutValidation;
    private LocalDate dateDepot;
    private String professionnelId;
    private Long tailleOctets;
    private String typeMime;
    private String urlVisualisation; // URL présignée temporaire (durée de vie limitée)
}
