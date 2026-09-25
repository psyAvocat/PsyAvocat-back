package com.psyavocat.dto.dossier;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepondreSoumissionRequest {

    @NotBlank(message = "Le statut de réponse est obligatoire (ACCEPTEE ou REFUSEE)")
    private String statut;

    private String reponse;

    private BigDecimal tarifPropose;
}
