package com.psyavocat.dto.referentiel;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Création / modification d'un tarif publié par le professionnel connecté (montants en FCFA). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifRequest {

    @NotBlank(message = "L'intitulé du tarif est obligatoire")
    @Size(max = 100, message = "L'intitulé ne doit pas dépasser 100 caractères")
    private String titre;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1", message = "Le montant doit être supérieur à 0")
    @DecimalMax(value = "10000000", message = "Le montant est trop élevé")
    private BigDecimal montant;

    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    @Min(value = 10, message = "La durée minimale est de 10 minutes")
    @Max(value = 480, message = "La durée maximale est de 8 heures")
    private Integer dureeMinutes;

    /** Publié (visible et réservable) ou non ; publié par défaut. */
    private Boolean actif;
}
