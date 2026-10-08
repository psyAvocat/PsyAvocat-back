package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Indicateurs clés (KPI) du tableau de bord administrateur.
 * Les quatre premiers champs sont historiques et conservés pour la rétrocompatibilité.
 * Les champs suivants ont été ajoutés de façon additive (aucun champ supprimé ou renommé).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalUtilisateurs;
    private long totalProfessionnels;
    private long professionnelsEnAttente;
    /** Nombre réel de rendez-vous dont la date se situe dans le mois civil courant. */
    private long rendezVousMoisCourant;

    // --- Champs additifs ---
    private long professionnelsValides;
    private long totalRendezVous;
    private long signalementsEnAttente;
}
