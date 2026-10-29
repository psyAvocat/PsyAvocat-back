package com.psyavocat.controller;

import com.psyavocat.dto.professionnel.CompteursNavigationDTO;
import com.psyavocat.service.CompteursNavigationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Données propres à l'espace du professionnel connecté (jamais publiques). */
@RestController
@RequestMapping("/api/espace-professionnel")
public class EspaceProfessionnelController {

    private final CompteursNavigationService compteursNavigationService;

    public EspaceProfessionnelController(CompteursNavigationService compteursNavigationService) {
        this.compteursNavigationService = compteursNavigationService;
    }

    /** Badges de la barre latérale : dossiers à examiner, nouveaux rendez-vous, messages non lus. */
    @GetMapping("/compteurs")
    @PreAuthorize("hasAnyRole('AVOCAT', 'PSYCHOLOGUE')")
    public ResponseEntity<CompteursNavigationDTO> getCompteurs() {
        return ResponseEntity.ok(compteursNavigationService.getCompteurs());
    }
}
