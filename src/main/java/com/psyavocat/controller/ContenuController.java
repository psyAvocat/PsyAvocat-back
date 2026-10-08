package com.psyavocat.controller;

import com.psyavocat.dto.contenu.ContenuRequest;
import com.psyavocat.dto.contenu.ContenuResponseDTO;
import com.psyavocat.dto.contenu.ContenuStatutRequest;
import com.psyavocat.service.ContenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Articles (avocats) et Conseils (psychologues).
 * Lecture : clients connectés. Gestion : auteur uniquement (vérifié dans le service).
 */
@RestController
@RequestMapping("/api/contenus")
public class ContenuController {

    private final ContenuService contenuService;

    public ContenuController(ContenuService contenuService) {
        this.contenuService = contenuService;
    }

    @GetMapping
    public ResponseEntity<List<ContenuResponseDTO>> rechercher(
            @RequestParam String type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String specialiteId,
            @RequestParam(required = false, defaultValue = "recent") String tri
    ) {
        return ResponseEntity.ok(contenuService.rechercher(type, q, specialiteId, tri));
    }

    @GetMapping("/mes-contenus")
    public ResponseEntity<List<ContenuResponseDTO>> getMesContenus() {
        return ResponseEntity.ok(contenuService.getMesContenus());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContenuResponseDTO> getContenu(@PathVariable String id) {
        return ResponseEntity.ok(contenuService.getContenu(id));
    }

    @PostMapping
    public ResponseEntity<ContenuResponseDTO> creer(@Valid @RequestBody ContenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contenuService.creer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContenuResponseDTO> modifier(@PathVariable String id, @Valid @RequestBody ContenuRequest request) {
        return ResponseEntity.ok(contenuService.modifier(id, request));
    }

    @PatchMapping("/{id}/actif")
    public ResponseEntity<ContenuResponseDTO> changerStatut(@PathVariable String id,
                                                            @Valid @RequestBody ContenuStatutRequest request) {
        return ResponseEntity.ok(contenuService.changerStatut(id, request.getActif()));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContenuResponseDTO> televerserImage(@PathVariable String id,
                                                              @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(contenuService.televerserImage(id, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        contenuService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
