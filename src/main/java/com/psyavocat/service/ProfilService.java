package com.psyavocat.service;

import com.psyavocat.dto.profil.*;

/**
 * Contrat de service pour la gestion des profils utilisateurs (Client, Avocat, Psychologue).
 */
public interface ProfilService {

    UserProfileResponse getCurrentProfile();

    /** Profil client unique (mobile), valable dans les univers Avocat et Psychologue. */
    UserProfileResponse createClientProfile(CreateClientRequest request);

    UserProfileResponse createAvocatProfile(CreateAvocatRequest request);

    UserProfileResponse createPsychologueProfile(CreatePsychologueRequest request);

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    /** Visite guidée de l'application terminée ou passée : elle n'est plus proposée à ce compte. */
    UserProfileResponse marquerVisiteGuideeVue();

    UserProfileResponse uploadPhotoProfil(org.springframework.web.multipart.MultipartFile file);

    UserProfileResponse supprimerPhotoProfil();
}
