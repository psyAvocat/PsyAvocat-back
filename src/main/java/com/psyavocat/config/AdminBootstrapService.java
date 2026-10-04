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


       

        // Idempotence : ne rien faire si un administrateur existe déjà
        long adminCount = administrateurRepository.count();
        if (adminCount > 0) {
            log.info(" [AdminBootstrap] {} administrateur(s) déjà présent(s) en base. Aucune action.", adminCount);
            return;
        }

        // Vérification que l'UID Firebase n'est pas déjà pris
        if (administrateurRepository.existsById(adminFirebaseUid)) {
            log.info("[AdminBootstrap] Administrateur avec UID '{}' déjà présent. Aucune action.", adminFirebaseUid);
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

    }
}
