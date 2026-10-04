package com.psyavocat.service;

import com.psyavocat.dto.admin.DashboardStatsDTO;
import com.psyavocat.dto.admin.StatistiquesDetailleesDTO;

/**
 * Statistiques réelles de la plateforme pour l'espace administrateur.
 * Toutes les valeurs sont calculées à partir de MySQL (aucune donnée simulée).
 */
public interface AdminStatistiquesService {

    int PERIODE_MIN_MOIS = 3;
    int PERIODE_MAX_MOIS = 24;

    DashboardStatsDTO getDashboardStats();

    /**
     * @param periodeMois nombre de mois civils couverts par les séries temporelles
     *                    (mois courant inclus), entre {@link #PERIODE_MIN_MOIS} et {@link #PERIODE_MAX_MOIS}.
     */
    StatistiquesDetailleesDTO getStatistiquesDetaillees(int periodeMois);
}
