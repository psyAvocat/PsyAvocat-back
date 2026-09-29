package com.psyavocat.config;

import com.psyavocat.config.seeder.AvailabilitySeeder;
import com.psyavocat.config.seeder.ProfessionalSeeder;
import com.psyavocat.config.seeder.QuestionnaireJuridiqueSeeder;
import com.psyavocat.config.seeder.QuestionnairePsychologiqueSeeder;
import com.psyavocat.config.seeder.ReferenceDataSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Orchestrateur principal de seeding au démarrage de PsyAvocat.
 *
 * Architecture modulaire & propre :
 * - ReferenceDataSeeder                : Domaines, Spécialités et Catégories de Besoin juridiques
 * - ProfessionalSeeder                 : Avocats et Psychologues avec leurs tarifs en FCFA
 * - AvailabilitySeeder                 : Créneaux horaires de disponibilités (10 jours glissants)
 * - QuestionnairePsychologiqueSeeder   : Questionnaire Psychologique V1 (lu depuis questionnaire_psychologique_v1.json)
 * - QuestionnaireJuridiqueSeeder       : Questionnaire Juridique V1
 *
 * Chaque seeder est 100% idempotent avec des vérifications granulaires par code/clé métier unique.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ReferenceDataSeeder referenceDataSeeder;
    private final ProfessionalSeeder professionalSeeder;
    private final AvailabilitySeeder availabilitySeeder;
    private final QuestionnairePsychologiqueSeeder questionnairePsychologiqueSeeder;
    private final QuestionnaireJuridiqueSeeder questionnaireJuridiqueSeeder;

    @Override
    public void run(String... args) {
        log.info("==========================================================================");
        log.info("🚀 [DataInitializer] Démarrage du Seeding Modulaire Idempotent PsyAvocat...");
        log.info("==========================================================================");

        try {
            // 1. Données de référence (Domaines, Spécialités, Catégories Juridiques)
            referenceDataSeeder.seed();

            // 2. Questionnaire Psychologique V1 (Catégories psychologiques, 10 Questions, 80 Réponses, Pondérations)
            questionnairePsychologiqueSeeder.seed();

            // 3. Questionnaire Juridique V1
            questionnaireJuridiqueSeeder.seed();

            // 4. Professionnels & Tarifs
            professionalSeeder.seed();

            // 5. Créneaux de disponibilités
            availabilitySeeder.seed();

            log.info("==========================================================================");
            log.info("✅ [DataInitializer] Synchronisation de la base de données terminée avec succès !");
            log.info("==========================================================================");
        } catch (Exception e) {
            log.error("❌ [DataInitializer] Erreur lors de l'exécution du seeding : {}", e.getMessage(), e);
        }
    }
}
