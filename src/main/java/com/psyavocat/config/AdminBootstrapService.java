package com.psyavocat.config;

import com.psyavocat.entity.Administrateur;
import com.psyavocat.repository.AdministrateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * AdminBootstrapService — PsyAvocat
 *
 * Mécanisme de bootstrap sécurisé et explicite de l'administrateur initial.
 *
 * Règles :
 * - Activé UNIQUEMENT si psyavocat.admin.bootstrap.enabled=true
 * - Idempotent : ne crée le compte que si aucun Administrateur n'existe déjà
 * - N'insert jamais de données métier (pas de spécialités, pros, questionnaires)
 * - Le Firebase UID et l'email doivent être fournis via variables d'environnement
 *   (ADMIN_FIREBASE_UID, ADMIN_EMAIL, ADMIN_NOM, ADMIN_PRENOM)
 * - Ne hardcode jamais de secret ou de mot de passe
 *
 * Usage pour la production / développement initial :
 *   1. Créer le compte dans la console Firebase Authentication
 *   2. Récupérer l'UID Firebase généré
 *   3. Définir les variables d'environnement (voir application.properties)
 *   4. Démarrer Spring Boot avec psyavocat.admin.bootstrap.enabled=true
 *   5. Vérifier que l'admin est créé dans MySQL
 *   6. Désactiver le bootstrap (passer à false ou supprimer la variable)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapService implements ApplicationRunner {

    private final AdministrateurRepository administrateurRepository;

    @Value("${psyavocat.admin.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${psyavocat.admin.firebase-uid:}")
    private String adminFirebaseUid;

    @Value("${psyavocat.admin.email:}")
    private String adminEmail;

    @Value("${psyavocat.admin.nom:Admin}")
    private String adminNom;

    @Value("${psyavocat.admin.prenom:PsyAvocat}")
    private String adminPrenom;

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.info("[AdminBootstrap] Bootstrap admin désactivé (psyavocat.admin.bootstrap.enabled=false).");
            return;
        }

        log.info("==========================================================================");
        log.info("🔐 [AdminBootstrap] Vérification du compte administrateur initial...");
        log.info("==========================================================================");

        // Validation des variables requises
        if (!StringUtils.hasText(adminFirebaseUid)) {
            log.error("❌ [AdminBootstrap] ADMIN_FIREBASE_UID est vide. Bootstrap annulé.");
            log.error("   → Définissez PSYAVOCAT_ADMIN_FIREBASE_UID dans vos variables d'environnement.");
            return;
        }
        if (!StringUtils.hasText(adminEmail)) {
            log.error("❌ [AdminBootstrap] ADMIN_EMAIL est vide. Bootstrap annulé.");
            log.error("   → Définissez PSYAVOCAT_ADMIN_EMAIL dans vos variables d'environnement.");
            return;
        }

        // Idempotence : ne rien faire si un administrateur existe déjà
        long adminCount = administrateurRepository.count();
        if (adminCount > 0) {
            log.info("✅ [AdminBootstrap] {} administrateur(s) déjà présent(s) en base. Aucune action.", adminCount);
            return;
        }

        // Vérification que l'UID Firebase n'est pas déjà pris
        if (administrateurRepository.existsById(adminFirebaseUid)) {
            log.info("✅ [AdminBootstrap] Administrateur avec UID '{}' déjà présent. Aucune action.", adminFirebaseUid);
            return;
        }

        // Création de l'administrateur initial
        Administrateur admin = new Administrateur();
        admin.setId(adminFirebaseUid);
        admin.setEmail(adminEmail);
        admin.setNom(adminNom);
        admin.setPrenom(adminPrenom);
        admin.setDateInscription(LocalDate.now());
        administrateurRepository.save(admin);

        log.info("==========================================================================");
        log.info("✅ [AdminBootstrap] Administrateur initial créé avec succès !");
        log.info("   UID Firebase : {}", adminFirebaseUid);
        log.info("   Email        : {}", adminEmail);
        log.info("   Nom          : {} {}", adminPrenom, adminNom);
        log.info("==========================================================================");
        log.warn("⚠️  [AdminBootstrap] DÉSACTIVEZ maintenant psyavocat.admin.bootstrap.enabled=false !");
    }
}
