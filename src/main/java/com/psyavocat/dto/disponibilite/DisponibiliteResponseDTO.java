package com.psyavocat.dto.disponibilite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibiliteResponseDTO {

    private String id;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String statut;
    private String professionnelId;
}
