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
    SignalementResponseDTO traiterSignalement(String id, String action);
    UtilisateurResponseDTO toggleStatutUtilisateur(String id, boolean actif);
}

