package com.psyavocat.service;

import com.psyavocat.dto.admin.DashboardStatsDTO;
import com.psyavocat.dto.admin.UtilisateurResponseDTO;

import java.util.List;

public interface AdminService {
    List<UtilisateurResponseDTO> getAllUtilisateurs();
    DashboardStatsDTO getDashboardStats();
}
