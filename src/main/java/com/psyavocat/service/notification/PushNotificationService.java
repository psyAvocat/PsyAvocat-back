package com.psyavocat.service.notification;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.psyavocat.entity.DeviceRegistration;
import com.psyavocat.repository.DeviceRegistrationRepository;
import com.psyavocat.service.FirebaseMessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Envoi des notifications push (FCM) vers TOUS les appareils actifs d'un utilisateur.
 *
 * Le push est attaché au compte et à l'appareil, jamais à l'univers affiché :
 * un rappel de rendez-vous Psychologue arrive même si l'app est dans l'univers Avocat.
 */
@Slf4j
@Service
public class PushNotificationService {

    /** Codes FCM indiquant un token définitivement inutilisable. */
    private static final Set<MessagingErrorCode> TOKENS_INVALIDES = Set.of(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
            MessagingErrorCode.SENDER_ID_MISMATCH
    );

    private final DeviceRegistrationRepository deviceRegistrationRepository;
    private final FirebaseMessagingService firebaseMessagingService;

    public PushNotificationService(DeviceRegistrationRepository deviceRegistrationRepository,
                                   FirebaseMessagingService firebaseMessagingService) {
        this.deviceRegistrationRepository = deviceRegistrationRepository;
        this.firebaseMessagingService = firebaseMessagingService;
    }

    @Transactional
    public void envoyerA(String utilisateurId, String titre, String message, Map<String, String> donnees) {
        List<DeviceRegistration> appareils = deviceRegistrationRepository.findByUtilisateurIdAndActifTrue(utilisateurId);
        if (appareils.isEmpty()) {
            return;
        }

        List<String> tokens = appareils.stream().map(DeviceRegistration::getToken).toList();
        BatchResponse reponse = firebaseMessagingService.sendToMultipleDevices(tokens, titre, message, donnees);
        if (reponse == null) {
            return;
        }

        List<String> tokensADesactiver = new ArrayList<>();
        List<SendResponse> reponses = reponse.getResponses();
        for (int i = 0; i < reponses.size() && i < tokens.size(); i++) {
            SendResponse r = reponses.get(i);
            if (!r.isSuccessful() && r.getException() != null
                    && TOKENS_INVALIDES.contains(r.getException().getMessagingErrorCode())) {
                tokensADesactiver.add(tokens.get(i));
            }
        }

        if (!tokensADesactiver.isEmpty()) {
            List<DeviceRegistration> perimes = deviceRegistrationRepository.findByTokenIn(tokensADesactiver);
            perimes.forEach(d -> d.setActif(false));
            deviceRegistrationRepository.saveAll(perimes);
            log.info("{} appareil(s) désactivé(s) (token FCM révoqué)", perimes.size());
        }
    }
}
