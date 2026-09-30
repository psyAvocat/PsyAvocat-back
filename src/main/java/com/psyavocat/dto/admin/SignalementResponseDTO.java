package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementResponseDTO {
    private String id;
    private String professionnelCibleNom;
    private String professionnelCibleId;
    private String auteurNom;
    private String motif;
    private String gravite;
    private String statut;
    private String description;
    private LocalDateTime dateSignalement;
}
