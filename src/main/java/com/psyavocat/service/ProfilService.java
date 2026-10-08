package com.psyavocat.service;

import com.psyavocat.dto.profil.*;

/**
 * Contrat de service pour la gestion des profils utilisateurs (Patient, Justiciable, Avocat, Psychologue).
 */
public interface ProfilService {

    UserProfileResponse getCurrentProfile();

    /** Profil client unique (mobile), valable dans les univers Avocat et Psychologue. */
    UserProfileResponse createClientProfile(CreateClientRequest request);

    UserProfileResponse createPatientProfile(CreatePatientRequest request);

    UserProfileResponse createJusticiableProfile(CreateJusticiableRequest request);

    UserProfileResponse createAvocatProfile(CreateAvocatRequest request);

    UserProfileResponse createPsychologueProfile(CreatePsychologueRequest request);

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    UserProfileResponse uploadPhotoProfil(org.springframework.web.multipart.MultipartFile file);

    UserProfileResponse supprimerPhotoProfil();
}
