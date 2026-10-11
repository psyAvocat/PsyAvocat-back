package com.psyavocat.dto.referentiel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifProfessionnelDTO {
    private String id;
    private String titre;
    private BigDecimal montant;
    private String devise;
    private String description;
    private Integer dureeMinutes;
    /** Publié (visible des clients) ; seuls les tarifs publiés figurent sur la fiche publique. */
    private Boolean actif;
}
