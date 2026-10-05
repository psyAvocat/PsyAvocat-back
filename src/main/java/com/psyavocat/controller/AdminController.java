package com.psyavocat.controller;

import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;
import com.psyavocat.dto.professionnel.StatutValidationRequest;
import com.psyavocat.service.ProfessionnelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class AdminController {

    private final ProfessionnelService professionnelService;
    private final com.psyavocat.service.AdminService adminService;
    private final com.psyavocat.service.AdminStatistiquesService adminStatistiquesService;

    public AdminController(ProfessionnelService professionnelService,
                           com.psyavocat.service.AdminService adminService,
                           com.psyavocat.service.AdminStatistiquesService adminStatistiquesService) {
        this.professionnelService = professionnelService;
        this.adminService = adminService;
        this.adminStatistiquesService = adminStatistiquesService;
    }

    @GetMapping("/utilisateurs")
    public ResponseEntity<List<com.psyavocat.dto.admin.UtilisateurResponseDTO>> getAllUtilisateurs() {
        return ResponseEntity.ok(adminService.getAllUtilisateurs());
    }

    @GetMapping("/utilisateurs/{id}")
    public ResponseEntity<com.psyavocat.dto.admin.UtilisateurResponseDTO> getUtilisateurById(@PathVariable String id) {
        return ResponseEntity.ok(adminService.getUtilisateurById(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<com.psyavocat.dto.admin.DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminStatistiquesService.getDashboardStats());
    }

    /**
     * Jeux de données réels pour les graphiques admin (séries mensuelles + répartitions).
     * @param mois nombre de mois couverts par les séries (3 à 24, défaut 12).
     */
    @GetMapping("/stats/details")
    public ResponseEntity<com.psyavocat.dto.admin.StatistiquesDetailleesDTO> getStatistiquesDetaillees(
            @RequestParam(name = "mois", defaultValue = "12") int mois) {
        return ResponseEntity.ok(adminStatistiquesService.getStatistiquesDetaillees(mois));
    }

    @GetMapping("/professionnels/en-attente")
    public ResponseEntity<List<ProfessionnelResponseDTO>> getProfessionnelsEnAttente() {
        return ResponseEntity.ok(professionnelService.getProfessionnelsEnAttente());
    }

    @GetMapping("/professionnels/{id}")
    public ResponseEntity<ProfessionnelResponseDTO> getProfessionnelById(@PathVariable String id) {
        return ResponseEntity.ok(professionnelService.getProfessionnelById(id));
    }

    @PatchMapping("/professionnels/{id}/statut")
    public ResponseEntity<ProfessionnelResponseDTO> updateStatutValidation(
            @PathVariable String id,
            @Valid @RequestBody StatutValidationRequest request
    ) {
        return ResponseEntity.ok(professionnelService.updateStatutValidation(id, request.getStatut(), request.getMotif()));
    }

    @PatchMapping("/utilisateurs/{id}/statut")
    public ResponseEntity<com.psyavocat.dto.admin.UtilisateurResponseDTO> toggleStatutUtilisateur(
            @PathVariable String id,
            @RequestParam(name = "actif") boolean actif) {
        return ResponseEntity.ok(adminService.toggleStatutUtilisateur(id, actif));
    }


    @GetMapping("/signalements")
    public ResponseEntity<List<com.psyavocat.dto.admin.SignalementResponseDTO>> getSignalementsActifs() {
        return ResponseEntity.ok(adminService.getSignalementsActifs());
    }

    @PatchMapping("/signalements/{id}/statut")
    public ResponseEntity<com.psyavocat.dto.admin.SignalementResponseDTO> traiterSignalement(
            @PathVariable String id,
            @RequestBody com.psyavocat.dto.admin.ActionSignalementRequest request) {
        return ResponseEntity.ok(adminService.traiterSignalement(id, request.getAction()));
    }
}
