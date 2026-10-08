package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Point d'une série temporelle mensuelle.
 * {@code mois} est au format ISO {@code yyyy-MM}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointMensuelDTO {
    private String mois;
    private long valeur;
}
