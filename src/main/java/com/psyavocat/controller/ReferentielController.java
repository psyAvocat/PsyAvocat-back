package com.psyavocat.controller;

import com.psyavocat.dto.referentiel.CategorieBesoinDTO;
import com.psyavocat.dto.referentiel.DomaineDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import com.psyavocat.service.ReferentielService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referentiels")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    // =========================================================================
    // DOMAINES
    // =========================================================================

    @GetMapping("/domaines")
    public ResponseEntity<List<DomaineDTO>> getDomaines() {
        return ResponseEntity.ok(referentielService.getAllDomaines());
    }

    @PostMapping("/domaines")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<DomaineDTO> createDomaine(@RequestBody DomaineDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referentielService.createDomaine(dto));
    }

    @PutMapping("/domaines/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<DomaineDTO> updateDomaine(@PathVariable String id, @RequestBody DomaineDTO dto) {
        return ResponseEntity.ok(referentielService.updateDomaine(id, dto));
    }

    @DeleteMapping("/domaines/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteDomaine(@PathVariable String id) {
        referentielService.deleteDomaine(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // SPÉCIALITÉS
    // =========================================================================

    @GetMapping("/specialites")
    public ResponseEntity<List<SpecialiteDTO>> getSpecialites(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(referentielService.getSpecialites(type));
    }

    @PostMapping("/specialites")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<SpecialiteDTO> createSpecialite(@RequestBody SpecialiteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referentielService.createSpecialite(dto));
    }

    @PutMapping("/specialites/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<SpecialiteDTO> updateSpecialite(@PathVariable String id, @RequestBody SpecialiteDTO dto) {
        return ResponseEntity.ok(referentielService.updateSpecialite(id, dto));
    }

    @DeleteMapping("/specialites/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteSpecialite(@PathVariable String id) {
        referentielService.deleteSpecialite(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // CATÉGORIES DE BESOIN
    // =========================================================================

    @GetMapping("/categories-besoin")
    public ResponseEntity<List<CategorieBesoinDTO>> getCategoriesBesoin(
            @RequestParam(required = false) String typeProfessionnel,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(referentielService.getCategoriesBesoin(typeProfessionnel, includeInactive));
    }

    @PatchMapping("/categories-besoin/{id}/toggle-actif")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<CategorieBesoinDTO> toggleActifCategorieBesoin(@PathVariable String id) {
        return ResponseEntity.ok(referentielService.toggleActifCategorieBesoin(id));
    }

    @PostMapping("/categories-besoin")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<CategorieBesoinDTO> createCategorieBesoin(@RequestBody CategorieBesoinDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referentielService.createCategorieBesoin(dto));
    }

    @PutMapping("/categories-besoin/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<CategorieBesoinDTO> updateCategorieBesoin(@PathVariable String id, @RequestBody CategorieBesoinDTO dto) {
        return ResponseEntity.ok(referentielService.updateCategorieBesoin(id, dto));
    }

    @DeleteMapping("/categories-besoin/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteCategorieBesoin(@PathVariable String id) {
        referentielService.deleteCategorieBesoin(id);
        return ResponseEntity.noContent().build();
    }
}
