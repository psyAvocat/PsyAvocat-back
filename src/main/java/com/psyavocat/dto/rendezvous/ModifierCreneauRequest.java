package com.psyavocat.dto.rendezvous;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Déplacement d'un rendez-vous sur un autre créneau libre du même professionnel. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModifierCreneauRequest {

    @NotBlank(message = "Le nouveau créneau est obligatoire")
    private String disponibiliteId;
}
