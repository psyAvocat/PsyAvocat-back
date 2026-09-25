package com.psyavocat.controller;

import com.psyavocat.dto.profil.*;
import com.psyavocat.service.ProfilService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profil")
public class ProfilController {

    private final ProfilService profilService;

    public ProfilController(ProfilService profilService) {
        this.profilService = profilService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {
        return ResponseEntity.ok(profilService.getCurrentProfile());
    }

    @PostMapping("/patient")
    public ResponseEntity<UserProfileResponse> createPatientProfile(@Valid @RequestBody CreatePatientRequest request) {
        UserProfileResponse response = profilService.createPatientProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/justiciable")
    public ResponseEntity<UserProfileResponse> createJusticiableProfile(@Valid @RequestBody CreateJusticiableRequest request) {
        UserProfileResponse response = profilService.createJusticiableProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/avocat")
    public ResponseEntity<UserProfileResponse> createAvocatProfile(@Valid @RequestBody CreateAvocatRequest request) {
        UserProfileResponse response = profilService.createAvocatProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/psychologue")
    public ResponseEntity<UserProfileResponse> createPsychologueProfile(@Valid @RequestBody CreatePsychologueRequest request) {
        UserProfileResponse response = profilService.createPsychologueProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profilService.updateProfile(request));
    }
}
