package com.psyavocat.dto.professionnel;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatutValidationRequest {

    @NotBlank(message = "Le statut est obligatoire (PENDING, APPROVED, REJECTED, SUSPENDED)")
    private String statut;
}
