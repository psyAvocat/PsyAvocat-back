package com.psyavocat.controller;

import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;
import com.psyavocat.service.ProfessionnelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionnels")
public class ProfessionnelController {

    private final ProfessionnelService professionnelService;

    public ProfessionnelController(ProfessionnelService professionnelService) {
        this.professionnelService = professionnelService;
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

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionnelResponseDTO> getProfessionnelById(@PathVariable String id) {
        return ResponseEntity.ok(professionnelService.getProfessionnelById(id));
    }
}
