package com.psyavocat.dto.signalement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.psyavocat.entity.MotifSignalement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Signalement d'un professionnel par le client connecté. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSignalementRequest {

    @NotBlank(message = "Le professionnel signalé est obligatoire")
    private String professionnelId;

    @NotNull(message = "Le motif est obligatoire")
    private MotifSignalement motif;

    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;
}
