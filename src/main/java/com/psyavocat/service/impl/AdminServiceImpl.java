package com.psyavocat.service.impl;

import com.psyavocat.dto.admin.DashboardStatsDTO;
import com.psyavocat.dto.admin.UtilisateurResponseDTO;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.repository.ProfessionnelRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.repository.RendezVousRepository;
import com.psyavocat.service.AdminService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final ProfessionnelRepository professionnelRepository;
    private final RendezVousRepository rendezVousRepository;

    public AdminServiceImpl(UtilisateurRepository utilisateurRepository,
                            ProfessionnelRepository professionnelRepository,
                            RendezVousRepository rendezVousRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.professionnelRepository = professionnelRepository;
        this.rendezVousRepository = rendezVousRepository;
    }

    @Override
    public List<UtilisateurResponseDTO> getAllUtilisateurs() {
        return utilisateurRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DashboardStatsDTO getDashboardStats() {
        long totalUsers = utilisateurRepository.count();
        long totalPros = professionnelRepository.count();
        long prosEnAttente = professionnelRepository.findByStatutValidation("PENDING").size();
        // Approximation pour le mois courant (on simplifie à count global pour éviter les complexités de date en JPA ici s'ils n'existent pas)
        long totalRdv = rendezVousRepository.count();

        return DashboardStatsDTO.builder()
                .totalUtilisateurs(totalUsers)
                .totalProfessionnels(totalPros)
                .professionnelsEnAttente(prosEnAttente)
                .rendezVousMoisCourant(totalRdv) // A adapter
                .build();
    }

    private UtilisateurResponseDTO mapToDTO(Utilisateur user) {
        String type = user.getClass().getSimpleName();
        return UtilisateurResponseDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .dateInscription(user.getDateInscription())
                .typeUtilisateur(type)
                .build();
    }
}
