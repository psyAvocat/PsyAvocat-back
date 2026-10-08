package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Élément d'une répartition (clé technique stable + effectif).
 * Le libellé affiché est géré côté client à partir de la clé.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionDTO {
    private String cle;
    private long valeur;
}
