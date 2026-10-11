package com.psyavocat.controller;

import com.psyavocat.dto.referentiel.TarifProfessionnelDTO;
import com.psyavocat.dto.referentiel.TarifRequest;
import com.psyavocat.service.TarifService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tarifs du professionnel connecté (gestion depuis son profil Angular). */
@RestController
@RequestMapping("/api/professionnel/tarifs")
@PreAuthorize("hasRole('PROFESSIONNEL')")
public class TarifController {

    private final TarifService tarifService;

    public TarifController(TarifService tarifService) {
        this.tarifService = tarifService;
    }

    @GetMapping
    public ResponseEntity<List<TarifProfessionnelDTO>> getMesTarifs() {
        return ResponseEntity.ok(tarifService.getMesTarifs());
    }

    @PostMapping
    public ResponseEntity<TarifProfessionnelDTO> creer(@Valid @RequestBody TarifRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarifService.creer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarifProfessionnelDTO> modifier(@PathVariable String id, @Valid @RequestBody TarifRequest request) {
        return ResponseEntity.ok(tarifService.modifier(id, request));
    }

    @PatchMapping("/{id}/actif")
    public ResponseEntity<TarifProfessionnelDTO> changerPublication(@PathVariable String id, @RequestParam boolean actif) {
        return ResponseEntity.ok(tarifService.changerPublication(id, actif));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        tarifService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
