package com.psyavocat.service.notification;

import com.psyavocat.dto.device.RegisterDeviceRequest;
import com.psyavocat.entity.DeviceRegistration;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.DeviceRegistrationRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Inscription, renouvellement et désactivation des appareils de l'utilisateur connecté. */
@Service
@Transactional
public class DeviceRegistrationService {

    private final DeviceRegistrationRepository deviceRegistrationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;

    public DeviceRegistrationService(DeviceRegistrationRepository deviceRegistrationRepository,
                                     UtilisateurRepository utilisateurRepository,
                                     AuthenticationContext authenticationContext) {
        this.deviceRegistrationRepository = deviceRegistrationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
    }

    /**
     * Enregistre l'appareil courant. Un token déjà connu est réattribué à l'utilisateur
     * connecté (cas d'un téléphone partagé après déconnexion / reconnexion).
     */
    public void enregistrer(RegisterDeviceRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        LocalDateTime maintenant = LocalDateTime.now();
        DeviceRegistration appareil = deviceRegistrationRepository.findByToken(request.getToken())
                .orElseGet(() -> {
                    DeviceRegistration nouveau = new DeviceRegistration();
                    nouveau.setToken(request.getToken());
                    nouveau.setDateCreation(maintenant);
                    return nouveau;
                });

        appareil.setUtilisateur(utilisateur);
        appareil.setPlateforme(request.getPlateforme());
        appareil.setActif(true);
        appareil.setDateDerniereActivite(maintenant);
        deviceRegistrationRepository.save(appareil);
    }

    /** Désactive l'appareil (déconnexion). Sans effet si le token n'appartient pas à l'utilisateur. */
    public void desactiver(String token) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        deviceRegistrationRepository.findByToken(token)
                .filter(d -> d.getUtilisateur() != null && uid.equals(d.getUtilisateur().getId()))
                .ifPresent(d -> {
                    d.setActif(false);
                    deviceRegistrationRepository.save(d);
                });
    }
}
