package com.psyavocat.controller;

import com.psyavocat.dto.disponibilite.CreateDisponibiliteRequest;
import com.psyavocat.dto.disponibilite.DisponibiliteResponseDTO;
import com.psyavocat.service.DisponibiliteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disponibilites")
public class DisponibiliteController {

    private final DisponibiliteService disponibiliteService;

    public DisponibiliteController(DisponibiliteService disponibiliteService) {
        this.disponibiliteService = disponibiliteService;
    }

    @PostMapping
    public ResponseEntity<DisponibiliteResponseDTO> createDisponibilite(@Valid @RequestBody CreateDisponibiliteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disponibiliteService.createDisponibilite(request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<DisponibiliteResponseDTO>> getMyDisponibilites() {
        return ResponseEntity.ok(disponibiliteService.getMyDisponibilites());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDisponibilite(@PathVariable String id) {
        disponibiliteService.deleteDisponibilite(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/professionnel/{professionnelId}")
    public ResponseEntity<List<DisponibiliteResponseDTO>> getDisponibilitesLibres(@PathVariable String professionnelId) {
        return ResponseEntity.ok(disponibiliteService.getDisponibilitesLibres(professionnelId));
    }
}
