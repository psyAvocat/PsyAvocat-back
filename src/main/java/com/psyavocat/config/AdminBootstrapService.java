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

/**
 * Création idempotente de l'administrateur initial au démarrage.
 *
 * Règles de sécurité :
 * - l'UID Firebase et l'e-mail proviennent de la configuration locale (.env), jamais du code ;
 * - aucun compte existant n'est jamais supprimé ni transformé : en cas de conflit
 *   (UID déjà utilisé par un autre type de compte, e-mail déjà pris), le bootstrap s'arrête
 *   et le signale pour une intervention manuelle ;
 * - rien n'est fait si l'administrateur existe déjà.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapService implements ApplicationRunner {

    private final AdministrateurRepository administrateurRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${psyavocat.admin.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${psyavocat.admin.firebase-uid:}")
    private String adminFirebaseUid;

    @Value("${psyavocat.admin.email:}")
    private String adminEmail;

    @Value("${psyavocat.admin.nom:admin}")
    private String adminNom;

    @Value("${psyavocat.admin.prenom:admin}")
    private String adminPrenom;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.info("[AdminBootstrap] Bootstrap administrateur désactivé.");
            return;
        }
        if (!StringUtils.hasText(adminFirebaseUid) || !StringUtils.hasText(adminEmail)) {
            log.warn("[AdminBootstrap] PSYAVOCAT_ADMIN_FIREBASE_UID ou PSYAVOCAT_ADMIN_EMAIL non renseigné : aucun administrateur initial créé.");
            return;
        }

        if (administrateurRepository.existsById(adminFirebaseUid)) {
            log.info("[AdminBootstrap] Administrateur initial déjà présent : aucune action.");
            return;
        }
        if (utilisateurRepository.existsById(adminFirebaseUid)) {
            log.error("[AdminBootstrap] L'UID configuré appartient déjà à un compte non administrateur : "
                    + "aucune modification automatique. Vérifiez la configuration.");
            return;
        }
        if (utilisateurRepository.findByEmail(adminEmail).isPresent()) {
            log.error("[AdminBootstrap] L'e-mail administrateur configuré est déjà associé à un autre compte : "
                    + "aucune suppression ni modification automatique. Intervention manuelle requise.");
            return;
        }

        Administrateur admin = new Administrateur();
        admin.setId(adminFirebaseUid);
        admin.setEmail(adminEmail);
        admin.setNom(StringUtils.hasText(adminNom) ? adminNom : "admin");
        admin.setPrenom(StringUtils.hasText(adminPrenom) ? adminPrenom : "admin");
        admin.setActif(true);
        admin.setDateInscription(LocalDate.now());
        administrateurRepository.save(admin);
        log.info("[AdminBootstrap] Administrateur initial créé.");
    }
}
