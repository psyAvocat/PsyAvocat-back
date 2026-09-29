package com.psyavocat.controller;

import com.psyavocat.dto.rendezvous.CreateRendezVousAvocatDirectRequest;
import com.psyavocat.dto.rendezvous.CreateRendezVousAvocatRequest;
import com.psyavocat.dto.rendezvous.CreateRendezVousPsyRequest;
import com.psyavocat.dto.rendezvous.RendezVousResponseDTO;
import com.psyavocat.service.RendezVousService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rendez-vous")
public class RendezVousController {

    private final RendezVousService rendezVousService;

    public RendezVousController(RendezVousService rendezVousService) {
        this.rendezVousService = rendezVousService;
    }

    @PostMapping("/psychologue")
    public ResponseEntity<RendezVousResponseDTO> createRendezVousPsychologue(
            @Valid @RequestBody CreateRendezVousPsyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.createRendezVousPsychologue(request));
    }

    @PostMapping("/avocat")
    public ResponseEntity<RendezVousResponseDTO> createRendezVousAvocat(
            @Valid @RequestBody CreateRendezVousAvocatRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.createRendezVousAvocat(request));
    }

    @PostMapping("/avocat/direct")
    public ResponseEntity<RendezVousResponseDTO> createRendezVousAvocatDirect(
            @Valid @RequestBody CreateRendezVousAvocatDirectRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.createRendezVousAvocatDirect(request));
    }

    @GetMapping
    public ResponseEntity<List<RendezVousResponseDTO>> getMyRendezVous() {
        return ResponseEntity.ok(rendezVousService.getMyRendezVous());
    }

    @PatchMapping("/{id}/annuler")
    public ResponseEntity<RendezVousResponseDTO> annulerRendezVous(@PathVariable String id) {
        return ResponseEntity.ok(rendezVousService.annulerRendezVous(id));
    }
}
