package com.psyavocat.controller;

import com.psyavocat.dto.referentiel.CategorieBesoinDTO;
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

    @GetMapping("/specialites")
    public ResponseEntity<List<SpecialiteDTO>> getSpecialites() {
        return ResponseEntity.ok(referentielService.getAllSpecialites());
    }

    @PostMapping("/specialites")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<SpecialiteDTO> createSpecialite(@RequestBody SpecialiteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referentielService.createSpecialite(dto));
    }

    @GetMapping("/categories-besoin")
    public ResponseEntity<List<CategorieBesoinDTO>> getCategoriesBesoin(
            @RequestParam(required = false) String typeProfessionnel
    ) {
        return ResponseEntity.ok(referentielService.getCategoriesBesoin(typeProfessionnel));
    }

    @PostMapping("/categories-besoin")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<CategorieBesoinDTO> createCategorieBesoin(@RequestBody CategorieBesoinDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referentielService.createCategorieBesoin(dto));
    }
}
