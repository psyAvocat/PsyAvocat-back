package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Jeux de données réels destinés aux graphiques de l'espace administrateur.
 * Les séries mensuelles couvrent la période [debut, fin] et contiennent un point
 * par mois (valeur 0 si aucune donnée en base pour ce mois).
 * Les répartitions reflètent l'état actuel complet de la base.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesDetailleesDTO {
    private int periodeMois;
    private LocalDate debut;
    private LocalDate fin;

    private List<PointMensuelDTO> inscriptionsParMois;
    private List<PointMensuelDTO> rendezVousParMois;

    private List<RepartitionDTO> utilisateursParProfil;
    private List<RepartitionDTO> professionnelsParStatut;
    private List<RepartitionDTO> rendezVousParStatut;
}
