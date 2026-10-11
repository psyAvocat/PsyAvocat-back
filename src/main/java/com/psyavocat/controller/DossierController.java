package com.psyavocat.controller;

import com.psyavocat.dto.dossier.*;
import com.psyavocat.service.DossierService;
import com.psyavocat.service.SoumissionDossierService;
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
    private final SoumissionDossierService soumissionDossierService;

    public DossierController(
            DossierService dossierService,
            SoumissionDossierService soumissionDossierService
    ) {
        this.dossierService = dossierService;
        this.soumissionDossierService = soumissionDossierService;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(soumissionDossierService.soumettreDossier(id, request));
    }

    @PostMapping(value = "/dossiers/{id}/pieces-jointes", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PieceJointeDTO> ajouterPieceJointe(
            @PathVariable String id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.ajouterPieceJointe(id, file));
    }

    @DeleteMapping("/dossiers/{id}/pieces-jointes/{pieceJointeId}")
    public ResponseEntity<Void> supprimerPieceJointe(@PathVariable String id, @PathVariable String pieceJointeId) {
        dossierService.supprimerPieceJointe(id, pieceJointeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avocat/soumissions")
    @PreAuthorize("hasRole('AVOCAT')")
    public ResponseEntity<List<SoumissionDossierResponseDTO>> getSoumissionsPourAvocat(
            @RequestParam(required = false) String statut
    ) {
        return ResponseEntity.ok(soumissionDossierService.getSoumissionsPourAvocat(statut));
    }

    @PatchMapping("/avocat/soumissions/{id}/repondre")
    @PreAuthorize("hasRole('AVOCAT')")
    public ResponseEntity<SoumissionDossierResponseDTO> repondreSoumission(
            @PathVariable String id,
            @Valid @RequestBody RepondreSoumissionRequest request
    ) {
        return ResponseEntity.ok(soumissionDossierService.repondreSoumission(id, request));
    }
}
