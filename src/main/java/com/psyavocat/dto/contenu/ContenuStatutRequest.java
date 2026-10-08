package com.psyavocat.dto.contenu;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContenuStatutRequest {

    @NotNull(message = "Le statut de publication est obligatoire")
    private Boolean actif;
}
