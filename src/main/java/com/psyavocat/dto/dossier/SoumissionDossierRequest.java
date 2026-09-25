package com.psyavocat.dto.dossier;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoumissionDossierRequest {

    @NotBlank(message = "L'identifiant de l'avocat destinataire est obligatoire")
    private String avocatId;
}
