package com.psyavocat.controller;

import com.psyavocat.dto.paiement.PaiementResponseDTO;
import com.psyavocat.service.PaiementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Historique des paiements (simulés en V1) du client connecté. */
@RestController
@RequestMapping("/api/paiements")
@PreAuthorize("hasRole('CLIENT') or hasRole('PATIENT') or hasRole('JUSTICIABLE')")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @GetMapping
    public ResponseEntity<List<PaiementResponseDTO>> getMesPaiements() {
        return ResponseEntity.ok(paiementService.getMesPaiements());
    }
}
