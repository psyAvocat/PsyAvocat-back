package com.psyavocat.dto.rendezvous;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRendezVousAvocatRequest {

    @NotBlank(message = "L'identifiant de la soumission acceptée est obligatoire")
    private String soumissionId;

    @NotBlank(message = "L'identifiant de la disponibilité est obligatoire")
    private String disponibiliteId;

    private String mode; // CABINET, VISIO
}
