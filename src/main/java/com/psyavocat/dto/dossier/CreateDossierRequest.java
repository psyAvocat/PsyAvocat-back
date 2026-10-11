package com.psyavocat.dto.dossier;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDossierRequest {

    @NotBlank(message = "Le titre du dossier est obligatoire")
    private String titre;

    private String description;

    /** Domaine juridique (spécialité AVOCAT du référentiel), facultatif. */
    private String specialiteId;
}
