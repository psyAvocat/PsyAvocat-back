package com.psyavocat.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration singleton du SDK Firebase Admin pour le backend PsyAvocat.
 *
 * Supporte :
 * 1. La variable d'environnement standard `GOOGLE_APPLICATION_CREDENTIALS`
 * 2. La propriété `firebase.credentials.path` (chemin vers le service account JSON hors dépôt)
 * 3. Les Application Default Credentials (ADC) de Google Cloud
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id:psyavocat}")
    private String projectId;

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Bean
    @ConditionalOnMissingBean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Réutilisation de l'instance FirebaseApp existante : {}", FirebaseApp.getInstance().getName());
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials = resolveCredentials();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("Firebase Admin initialisé avec succès pour le projet '{}'", projectId);
        return app;
    }

    @Bean
    @ConditionalOnMissingBean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    @ConditionalOnMissingBean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        // 1. Vérifier si un chemin explicite a été configuré via application.properties / ENV
        if (StringUtils.hasText(credentialsPath)) {
            File credentialsFile = new File(credentialsPath);
            if (!credentialsFile.exists()) {
                throw new IllegalStateException(
                        "Le fichier de clés Firebase Admin spécifié n'existe pas : " + credentialsPath
                );
            }
            try (InputStream is = new FileInputStream(credentialsFile)) {
                log.info("Chargement des credentials Firebase depuis le fichier : {}", credentialsFile.getAbsolutePath());
                return GoogleCredentials.fromStream(is);
            }
        }

        // 2. Vérifier la variable GOOGLE_APPLICATION_CREDENTIALS
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(envPath)) {
            File envFile = new File(envPath);
            if (envFile.exists()) {
                try (InputStream is = new FileInputStream(envFile)) {
                    log.info("Chargement des credentials Firebase via GOOGLE_APPLICATION_CREDENTIALS : {}", envFile.getAbsolutePath());
                    return GoogleCredentials.fromStream(is);
                }
            } else {
                throw new IllegalStateException(
                        "Le fichier spécifié par GOOGLE_APPLICATION_CREDENTIALS n'existe pas : " + envPath
                );
            }
        }

        // 3. Application Default Credentials (environnement de déploiement Cloud ou gcloud auth)
        try {
            log.info("Tentative de chargement via Google Application Default Credentials...");
            return GoogleCredentials.getApplicationDefault();
        } catch (IOException e) {
            log.warn("Application Default Credentials introuvables. Initialisation sans credentials explicites pour environnement local/test.");
            // En environnement de test sans credentials cloud, génère des credentials vides
            return GoogleCredentials.create(null);
        }
    }
}
