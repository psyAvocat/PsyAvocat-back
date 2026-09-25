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
public class EcheanceDTO {
    private String id;
    private String description;
    private LocalDate date;
    private String statut;
}
