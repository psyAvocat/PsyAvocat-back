package com.psyavocat.dto.rendezvous;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRendezVousPsyRequest {

    @NotBlank(message = "L'identifiant du psychologue est obligatoire")
    private String psychologueId;

    @NotBlank(message = "L'identifiant de la disponibilité est obligatoire")
    private String disponibiliteId;

    private String mode; // CABINET, VISIO

    @NotNull(message = "Le montant total de la consultation est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant total doit être supérieur à zéro")
    private BigDecimal montantTotal;
}
