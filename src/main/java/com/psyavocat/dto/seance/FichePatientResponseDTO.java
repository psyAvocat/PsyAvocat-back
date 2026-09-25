package com.psyavocat.dto.seance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FichePatientResponseDTO {

    private String id;
    private LocalDate dateCreation;
    private String patientId;
    private String patientNom;
    private String patientPrenom;
    private String patientEmail;
    private String patientTelephone;
    private List<SeanceResponseDTO> seances;
}
