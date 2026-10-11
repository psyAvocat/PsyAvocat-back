package com.psyavocat.config;

import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Préchargement des clés publiques Google qui servent à vérifier les jetons Firebase.
 *
 * Sans lui, la première requête authentifiée après le démarrage télécharge ces clés
 * (plusieurs secondes sur une connexion lente) et peut dépasser le délai d'attente
 * de l'application mobile. Le SDK les garde ensuite en cache.
 *
 * Le SDK retélécharge les clés à leur expiration (toutes les quelques heures) : un
 * rafraîchissement périodique en arrière-plan évite que ce téléchargement tombe sur
 * la requête d'un utilisateur. Hors de cette fenêtre, l'appel est instantané (cache).
 *
 * Principe : un jeton d'amorçage, aux revendications valides mais non signé, est
 * soumis au SDK. Le SDK télécharge les clés pour vérifier la signature, puis rejette
 * le jeton : ce rejet est attendu. Aucun utilisateur n'est jamais authentifié ainsi.
 */
@Slf4j
@Component
public class FirebaseKeysWarmup {

    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;

    @Value("${firebase.project-id:psyavocat}")
    private String projectId;

    public FirebaseKeysWarmup(ObjectProvider<FirebaseAuth> firebaseAuthProvider) {
        this.firebaseAuthProvider = firebaseAuthProvider;
    }

    /** Au démarrage, en arrière-plan : l'API répond déjà pendant le téléchargement. */
    @EventListener(ApplicationReadyEvent.class)
    public void prechargerAuDemarrage() {
        Thread thread = new Thread(this::precharger, "firebase-keys-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Toutes les 4 minutes : le SDK renouvelle ses clés dans les 5 minutes qui
     * précèdent leur expiration ; ce passage régulier déclenche ce renouvellement
     * hors des requêtes des utilisateurs.
     */
    @Scheduled(initialDelay = 240_000, fixedDelay = 240_000)
    public void rafraichir() {
        precharger();
    }

    private void precharger() {
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            return;
        }
        long debut = System.currentTimeMillis();
        try {
            firebaseAuth.verifyIdToken(jetonAmorce());
        } catch (Exception attendu) {
            // Rejet attendu : seul le téléchargement des clés compte.
        }
        long duree = System.currentTimeMillis() - debut;
        if (duree > 1_000) {
            log.info("[Firebase] Clés publiques de vérification des jetons (re)chargées en {} ms", duree);
        }
    }

    private String jetonAmorce() {
        long maintenant = Instant.now().getEpochSecond();
        String entete = "{\"alg\":\"RS256\",\"kid\":\"psyavocat-warmup\",\"typ\":\"JWT\"}";
        String contenu = "{\"aud\":\"" + projectId + "\",\"iss\":\"https://securetoken.google.com/" + projectId
                + "\",\"sub\":\"warmup\",\"iat\":" + (maintenant - 60) + ",\"exp\":" + (maintenant + 3600)
                + ",\"auth_time\":" + (maintenant - 60) + "}";
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString(entete.getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(contenu.getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString("signature".getBytes(StandardCharsets.UTF_8));
    }
}
