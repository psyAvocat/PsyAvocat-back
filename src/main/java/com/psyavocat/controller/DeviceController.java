package com.psyavocat.controller;

import com.psyavocat.dto.device.RegisterDeviceRequest;
import com.psyavocat.service.notification.DeviceRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Appareils de l'utilisateur connecté, pour les notifications push FCM. */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceRegistrationService deviceRegistrationService;

    public DeviceController(DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
    }

    /** Enregistrement ou renouvellement du token FCM de l'appareil. */
    @PostMapping
    public ResponseEntity<Void> enregistrer(@Valid @RequestBody RegisterDeviceRequest request) {
        deviceRegistrationService.enregistrer(request);
        return ResponseEntity.noContent().build();
    }

    /** Désactivation à la déconnexion (le token est passé en paramètre : il contient des « : »). */
    @DeleteMapping
    public ResponseEntity<Void> desactiver(@RequestParam String token) {
        deviceRegistrationService.desactiver(token);
        return ResponseEntity.noContent().build();
    }
}
