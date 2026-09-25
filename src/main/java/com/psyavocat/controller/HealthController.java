package com.psyavocat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint public de santé de l'API PsyAvocat.
 * Accessible sans authentification.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "psyAvocat");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
