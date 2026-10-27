package com.psyavocat.service.impl;

import com.psyavocat.dto.professionnel.CompteursNavigationDTO;
import com.psyavocat.entity.Avocat;
import com.psyavocat.repository.MessageContactRepository;
import com.psyavocat.repository.NotificationRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.CompteursNavigationService;
import com.psyavocat.service.notification.RendezVousEvenements;
import com.psyavocat.service.support.Entites;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Éléments nouveaux qui demandent l'attention du professionnel, d'après les événements réels :
 * - dossiers reçus non vus (notifications « dossier reçu » non lues, avocat) ;
 * - événements de rendez-vous non vus (notifications « rendez-vous » non lues) ;
 * - messages non lus.
 * Ouvrir la page concernée marque ses notifications comme lues : le badge disparaît.
 */
@Service
@Transactional(readOnly = true)
public class CompteursNavigationServiceImpl implements CompteursNavigationService {

    private final AuthenticationContext authenticationContext;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationRepository notificationRepository;
    private final MessageContactRepository messageContactRepository;

    public CompteursNavigationServiceImpl(AuthenticationContext authenticationContext,
                                          UtilisateurRepository utilisateurRepository,
                                          NotificationRepository notificationRepository,
                                          MessageContactRepository messageContactRepository) {
        this.authenticationContext = authenticationContext;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationRepository = notificationRepository;
        this.messageContactRepository = messageContactRepository;
    }

    @Override
    public CompteursNavigationDTO getCompteurs() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        boolean avocat = utilisateurRepository.findById(uid)
                .map(u -> Entites.reel(u) instanceof Avocat)
                .orElse(false);
        return CompteursNavigationDTO.builder()
                .dossiersNouveaux(avocat ? notificationRepository.countByDestinataireIdAndRessourceTypeAndLuFalse(
                        uid, SoumissionDossierServiceImpl.RESSOURCE_SOUMISSION) : 0)
                .rendezVousNouveaux(notificationRepository.countByDestinataireIdAndRessourceTypeAndLuFalse(
                        uid, RendezVousEvenements.RESSOURCE_RENDEZ_VOUS))
                .messagesNonLus(messageContactRepository.countUnreadMessagesForUser(uid))
                .build();
    }
}
