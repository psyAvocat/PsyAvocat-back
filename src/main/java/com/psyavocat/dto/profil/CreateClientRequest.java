package com.psyavocat.dto.profil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Création du profil client unique (application mobile), valable dans les deux univers.
 * Le format et l'unicité du téléphone sont vérifiés par le service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 80, message = "Le nom ne doit pas dépasser 80 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 80, message = "Le prénom ne doit pas dépasser 80 caractères")
    private String prenom;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String telephone;
}
