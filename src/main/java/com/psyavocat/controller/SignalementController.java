package com.psyavocat.controller;

import com.psyavocat.dto.signalement.CreateSignalementRequest;
import com.psyavocat.dto.signalement.MotifSignalementDTO;
import com.psyavocat.dto.signalement.SignalementCreeDTO;
import com.psyavocat.service.SignalementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Signalements créés par les clients (le traitement se fait via /api/admin/signalements). */
@RestController
@RequestMapping("/api/signalements")
public class SignalementController {

    private final SignalementService signalementService;

    public SignalementController(SignalementService signalementService) {
        this.signalementService = signalementService;
    }

    @GetMapping("/motifs")
    public ResponseEntity<List<MotifSignalementDTO>> getMotifs() {
        return ResponseEntity.ok(signalementService.getMotifs());
    }

    @PostMapping
    public ResponseEntity<SignalementCreeDTO> signaler(@Valid @RequestBody CreateSignalementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signalementService.signalerProfessionnel(request));
    }
}
