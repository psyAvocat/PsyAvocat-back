package com.psyavocat.dto.contenu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Création / modification d'un contenu par son auteur.
 * Le type (ARTICLE / CONSEIL) n'est PAS choisi par le client : il découle du profil de l'auteur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContenuRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String titre;

    @Size(max = 500, message = "Le résumé ne doit pas dépasser 500 caractères")
    private String description;

    @NotBlank(message = "Le contenu est obligatoire")
    @Size(max = 50000, message = "Le contenu est trop long")
    private String contenu;

    /** Spécialité du référentiel (facultative), du même univers que l'auteur. */
    private String specialiteId;

    /** Publier immédiatement (true) ou enregistrer en brouillon inactif (false). */
    private Boolean actif = Boolean.TRUE;
}
