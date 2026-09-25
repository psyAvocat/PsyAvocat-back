package com.psyavocat.controller;

import com.psyavocat.dto.dossier.*;
import com.psyavocat.service.DossierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DossierController {

    private final DossierService dossierService;

    public DossierController(DossierService dossierService) {
        this.dossierService = dossierService;
    }

    @PostMapping("/dossiers")
    public ResponseEntity<DossierResponseDTO> createDossier(@Valid @RequestBody CreateDossierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.createDossier(request));
    }

    @GetMapping("/dossiers")
    public ResponseEntity<List<DossierResponseDTO>> getMyDossiers() {
        return ResponseEntity.ok(dossierService.getMyDossiers());
    }

    @GetMapping("/dossiers/{id}")
    public ResponseEntity<DossierResponseDTO> getDossierById(@PathVariable String id) {
        return ResponseEntity.ok(dossierService.getDossierById(id));
    }

    @PostMapping("/dossiers/{id}/soumissions")
    public ResponseEntity<SoumissionDossierResponseDTO> soumettreDossier(
            @PathVariable String id,
            @Valid @RequestBody SoumissionDossierRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.soumettreDossier(id, request));
    }

    @GetMapping("/avocat/soumissions")
    @PreAuthorize("hasRole('AVOCAT')")
    public ResponseEntity<List<SoumissionDossierResponseDTO>> getSoumissionsPourAvocat(
            @RequestParam(required = false) String statut
    ) {
        return ResponseEntity.ok(dossierService.getSoumissionsPourAvocat(statut));
    }

    @PatchMapping("/avocat/soumissions/{id}/repondre")
    @PreAuthorize("hasRole('AVOCAT')")
    public ResponseEntity<SoumissionDossierResponseDTO> repondreSoumission(
            @PathVariable String id,
            @Valid @RequestBody RepondreSoumissionRequest request
    ) {
        return ResponseEntity.ok(dossierService.repondreSoumission(id, request));
    }
}
