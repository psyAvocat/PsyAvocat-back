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

    public AdminController(ProfessionnelService professionnelService) {
        this.professionnelService = professionnelService;
    }

    @GetMapping("/professionnels/en-attente")
    public ResponseEntity<List<ProfessionnelResponseDTO>> getProfessionnelsEnAttente() {
        return ResponseEntity.ok(professionnelService.getProfessionnelsEnAttente());
    }

    @PatchMapping("/professionnels/{id}/statut")
    public ResponseEntity<ProfessionnelResponseDTO> updateStatutValidation(
            @PathVariable String id,
            @Valid @RequestBody StatutValidationRequest request
    ) {
        return ResponseEntity.ok(professionnelService.updateStatutValidation(id, request.getStatut()));
    }
}
