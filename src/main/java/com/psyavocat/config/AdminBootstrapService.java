package com.psyavocat.config;

import com.psyavocat.entity.Administrateur;
import com.psyavocat.repository.AdministrateurRepository;
import com.psyavocat.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapService implements ApplicationRunner {

    private final AdministrateurRepository administrateurRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${psyavocat.admin.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${psyavocat.admin.firebase-uid:aW1omdSLFBWOQl9TAaSwPNrLDeF3}")
    private String adminFirebaseUid;

    @Value("${psyavocat.admin.email:admin@psyavocat.com}")
    private String adminEmail;

    @Value("${psyavocat.admin.nom:admin}")
    private String adminNom;

    @Value("${psyavocat.admin.prenom:admin}")
    private String adminPrenom;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.info("[AdminBootstrap] Bootstrap admin désactivé (psyavocat.admin.bootstrap.enabled=false).");
            return;
        }

        if (!StringUtils.hasText(adminFirebaseUid) || !StringUtils.hasText(adminEmail)) {
            log.warn("[AdminBootstrap] UID Firebase ou Email admin non renseigné. Abandon du bootstrap.");
            return;
        }

        // Si l'administrateur avec cet UID exact existe déjà
        if (administrateurRepository.existsById(adminFirebaseUid)) {
            log.info("[AdminBootstrap] L'administrateur avec l'UID '{}' existe déjà en base. Aucune action.", adminFirebaseUid);
            return;
        }

        // Si un compte avec cet email existe déjà avec un ancien ou différent UID
        utilisateurRepository.findByEmail(adminEmail).ifPresent(existingUser -> {
            log.warn("[AdminBootstrap] Un compte avec l'email '{}' existe déjà avec l'ancien UID '{}'. Remplacement pour associer le vrai UID Firebase...",
                    adminEmail, existingUser.getId());
            utilisateurRepository.delete(existingUser);
            utilisateurRepository.flush();
        });

        // Création de l'administrateur initial avec le vrai UID Firebase
        Administrateur admin = new Administrateur();
        admin.setId(adminFirebaseUid);
        admin.setEmail(adminEmail);
        admin.setNom(StringUtils.hasText(adminNom) ? adminNom : "admin");
        admin.setPrenom(StringUtils.hasText(adminPrenom) ? adminPrenom : "admin");
        admin.setActif(true);
        admin.setDateInscription(LocalDate.now());

        administrateurRepository.save(admin);
        log.info("[AdminBootstrap] ✅ Administrateur initialisé avec succès ! UID Firebase: '{}', Email: '{}', Nom: '{}', Prénom: '{}'.",
                adminFirebaseUid, adminEmail, admin.getNom(), admin.getPrenom());
    }
}

