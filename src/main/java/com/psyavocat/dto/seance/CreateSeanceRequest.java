package com.psyavocat.dto.seance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeanceRequest {

    @NotBlank(message = "L'identifiant du patient est obligatoire")
    private String patientId;

    @NotNull(message = "La date et l'heure de la séance sont obligatoires")
    private LocalDateTime date;

    private String noteInitiale;
}
