package com.psyavocat.service;

import com.psyavocat.dto.admin.DashboardStatsDTO;
import com.psyavocat.dto.admin.UtilisateurResponseDTO;
import com.psyavocat.dto.admin.SignalementResponseDTO;

import java.util.List;

public interface AdminService {
    List<UtilisateurResponseDTO> getAllUtilisateurs();
    UtilisateurResponseDTO getUtilisateurById(String id);
    DashboardStatsDTO getDashboardStats();
    List<SignalementResponseDTO> getSignalementsActifs();
    SignalementResponseDTO traiterSignalement(String id, String action);
}
