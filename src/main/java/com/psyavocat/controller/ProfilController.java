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

    @GetMapping({"", "/me"})
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {
        return ResponseEntity.ok(profilService.getCurrentProfile());
    }

    /** Profil client unique créé à l'inscription mobile (univers Avocat + Psychologue). */
    @PostMapping("/client")
    public ResponseEntity<UserProfileResponse> createClientProfile(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profilService.createClientProfile(request));
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

    @PutMapping({"", "/me"})
    public ResponseEntity<UserProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profilService.updateProfile(request));
    }

    @PostMapping(value = "/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadPhotoProfil(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(profilService.uploadPhotoProfil(file));
    }

    @DeleteMapping("/photo")
    public ResponseEntity<UserProfileResponse> supprimerPhotoProfil() {
        return ResponseEntity.ok(profilService.supprimerPhotoProfil());
    }
}
