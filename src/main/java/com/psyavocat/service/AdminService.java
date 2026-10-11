package com.psyavocat.service;

import com.psyavocat.dto.admin.UtilisateurResponseDTO;
import com.psyavocat.dto.admin.SignalementResponseDTO;

import java.util.List;

/**
 * Les statistiques du tableau de bord sont gérées par {@link AdminStatistiquesService}.
 */
public interface AdminService {
    List<UtilisateurResponseDTO> getAllUtilisateurs();
    UtilisateurResponseDTO getUtilisateurById(String id);
    List<SignalementResponseDTO> getSignalementsActifs();

    /** Signalements filtrés par statut (null : tous), du plus récent au plus ancien. */
    List<SignalementResponseDTO> getSignalements(String statut);

    SignalementResponseDTO traiterSignalement(String id, String action, String motif);

    /** Historique des décisions administratives concernant un élément. */
    List<com.psyavocat.dto.admin.DecisionAdministrativeDTO> getHistoriqueDecisions(String cibleId);
    UtilisateurResponseDTO toggleStatutUtilisateur(String id, boolean actif);
}

