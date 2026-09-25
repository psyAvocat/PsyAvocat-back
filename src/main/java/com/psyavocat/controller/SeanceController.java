package com.psyavocat.controller;

import com.psyavocat.dto.seance.*;
import com.psyavocat.service.SeanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/psychologue")
@PreAuthorize("hasRole('PSYCHOLOGUE')")
public class SeanceController {

    private final SeanceService seanceService;

    public SeanceController(SeanceService seanceService) {
        this.seanceService = seanceService;
    }

    @GetMapping("/fiches-patient")
    public ResponseEntity<List<FichePatientResponseDTO>> getMesFichesPatient() {
        return ResponseEntity.ok(seanceService.getMesFichesPatient());
    }

    @GetMapping("/fiches-patient/{patientId}")
    public ResponseEntity<FichePatientResponseDTO> getFichePatient(@PathVariable String patientId) {
        return ResponseEntity.ok(seanceService.getFichePatient(patientId));
    }

    @PostMapping("/seances")
    public ResponseEntity<SeanceResponseDTO> createSeance(@Valid @RequestBody CreateSeanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seanceService.createSeance(request));
    }

    @PostMapping("/seances/{id}/notes")
    public ResponseEntity<NoteSeanceDTO> ajouterNoteSeance(
            @PathVariable String id,
            @Valid @RequestBody CreateNoteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seanceService.ajouterNoteSeance(id, request));
    }
}
