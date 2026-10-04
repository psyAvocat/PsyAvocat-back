package com.psyavocat.controller;

import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;
import com.psyavocat.service.ProfessionnelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/professionnels", "/api/professionnel"})
public class ProfessionnelController {

    private final ProfessionnelService professionnelService;
    private final com.psyavocat.service.ProfessionnelEspaceService professionnelEspaceService;

    public ProfessionnelController(
            ProfessionnelService professionnelService,
            com.psyavocat.service.ProfessionnelEspaceService professionnelEspaceService
    ) {
        this.professionnelService = professionnelService;
        this.professionnelEspaceService = professionnelEspaceService;
    }

    @GetMapping
    public ResponseEntity<List<ProfessionnelResponseDTO>> searchProfessionnels(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String specialiteId,
            @RequestParam(required = false) String modeConsultation
    ) {
        return ResponseEntity.ok(professionnelService.searchProfessionnels(type, ville, specialiteId, modeConsultation));
    }

    @GetMapping("/clients")
    public ResponseEntity<List<com.psyavocat.dto.professionnel.ProfessionnelClientDTO>> getMesClients() {
        return ResponseEntity.ok(professionnelEspaceService.getMesClients());
    }

    @GetMapping("/stats")
    public ResponseEntity<com.psyavocat.dto.professionnel.ProfessionnelStatistiquesDTO> getMesStatistiques(
            @RequestParam(defaultValue = "6") int mois
    ) {
        return ResponseEntity.ok(professionnelEspaceService.getMesStatistiques(mois));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionnelResponseDTO> getProfessionnelById(@PathVariable String id) {
        return ResponseEntity.ok(professionnelService.getProfessionnelById(id));
    }
}
