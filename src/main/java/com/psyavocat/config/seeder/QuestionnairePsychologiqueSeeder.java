package com.psyavocat.config.seeder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyavocat.entity.*;
import com.psyavocat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Seeder dédié au Questionnaire Psychologique V1.
 * Source de données : src/main/resources/data/questionnaires/questionnaire_psychologique_v1.json
 *
 * Responsabilités :
 * 1. Charger et parser le fichier JSON officiel.
 * 2. Créer / synchroniser de manière 100% idempotente les 8 Catégories de Besoin psychologiques.
 * 3. Créer / synchroniser le Questionnaire (code: QUESTIONNAIRE_ORIENTATION_PSY_V1).
 * 4. Créer les 10 Questions obligatoires avec leur code unique (Q01 à Q10).
 * 5. Créer les 80 Réponses associées (Q01_R01 à Q10_R08).
 * 6. Créer les 80 PonderationOrientation reliant chaque réponse à sa catégorie cible avec son poids exact.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionnairePsychologiqueSeeder {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CategorieBesoinRepository categorieBesoinRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final ReponseRepository reponseRepository;
    private final PonderationOrientationRepository ponderationOrientationRepository;

    private static final String JSON_PATH = "data/questionnaires/questionnaire_psychologique_v1.json";

    @Transactional
    public void seed() {
        log.info("  [QuestionnairePsychologiqueSeeder] Initialisation depuis {}...", JSON_PATH);

        try {
            ClassPathResource resource = new ClassPathResource(JSON_PATH);
            if (!resource.exists()) {
                log.error("Fichier de questionnaire introuvable dans le classpath: {}", JSON_PATH);
                return;
            }

            JsonNode root;
            try (InputStream is = resource.getInputStream()) {
                root = objectMapper.readTree(is);
            }

            // 1. Synchronisation des 8 Catégories de Besoin
            Map<String, CategorieBesoin> categoriesByCode = seedCategories(root.path("categories"));

            // 2. Synchronisation du Questionnaire, des Questions, Réponses et Pondérations
            seedQuestionnaire(root.path("questionnaire"), categoriesByCode);

            log.info("  [QuestionnairePsychologiqueSeeder] Initialisation terminée avec succès.");

        } catch (Exception e) {
            log.error("Erreur lors du seeding du questionnaire psychologique : {}", e.getMessage(), e);
            throw new RuntimeException("Échec du seeding du questionnaire psychologique", e);
        }
    }

    private Map<String, CategorieBesoin> seedCategories(JsonNode categoriesNode) {
        Map<String, CategorieBesoin> map = new HashMap<>();

        if (categoriesNode.isArray()) {
            for (JsonNode catNode : categoriesNode) {
                String code = catNode.path("code").asText();
                String nom = catNode.path("nom").asText();
                String description = catNode.path("description").asText();
                String typeProfessionnel = catNode.path("typeProfessionnel").asText("PSYCHOLOGUE");
                boolean actif = catNode.path("actif").asBoolean(true);

                CategorieBesoin cat = categorieBesoinRepository.findByCode(code)
                        .orElseGet(() -> categorieBesoinRepository.findByNom(nom).orElse(null));

                if (cat == null) {
                    cat = new CategorieBesoin();
                    cat.setCode(code);
                    cat.setNom(nom);
                    cat.setDescription(description);
                    cat.setTypeProfessionnel(typeProfessionnel);
                    cat.setActif(actif);
                    cat = categorieBesoinRepository.save(cat);
                    log.info("    + Catégorie psychologique créée : [{}] {}", code, nom);
                } else {
                    boolean modified = false;
                    if (cat.getCode() == null || !cat.getCode().equals(code)) {
                        cat.setCode(code);
                        modified = true;
                    }
                    if (!nom.equals(cat.getNom())) {
                        cat.setNom(nom);
                        modified = true;
                    }
                    if (modified) {
                        cat = categorieBesoinRepository.save(cat);
                    }
                }
                map.put(code, cat);
            }
        }
        return map;
    }

    private void seedQuestionnaire(JsonNode questionnaireNode, Map<String, CategorieBesoin> categoriesByCode) {
        String qCode = questionnaireNode.path("code").asText("QUESTIONNAIRE_ORIENTATION_PSY_V1");
        String qTitre = questionnaireNode.path("titre").asText("Questionnaire d’orientation psychologique");
        String qType = questionnaireNode.path("type").asText("PSYCHOLOGIQUE");
        boolean qActif = questionnaireNode.path("actif").asBoolean(true);

        Questionnaire questionnaire = questionnaireRepository.findByCode(qCode)
                .orElseGet(() -> questionnaireRepository.findByType(qType).orElse(null));

        if (questionnaire == null) {
            questionnaire = new Questionnaire();
            questionnaire.setCode(qCode);
            questionnaire.setTitre(qTitre);
            questionnaire.setType(qType);
            questionnaire.setActif(qActif);
            questionnaire = questionnaireRepository.save(questionnaire);
            log.info("    + Questionnaire créé : [{}] {}", qCode, qTitre);
        } else {
            if (questionnaire.getCode() == null) {
                questionnaire.setCode(qCode);
                questionnaire.setTitre(qTitre);
                questionnaire = questionnaireRepository.save(questionnaire);
            }
        }

        JsonNode questionsNode = questionnaireNode.path("questions");
        if (questionsNode.isArray()) {
            for (JsonNode questNode : questionsNode) {
                seedQuestion(questionnaire, questNode, categoriesByCode);
            }
        }
    }

    private void seedQuestion(Questionnaire questionnaire, JsonNode questNode, Map<String, CategorieBesoin> categoriesByCode) {
        String questCode = questNode.path("code").asText();
        String questTexte = questNode.path("texte").asText();
        int questOrdre = questNode.path("ordre").asInt();
        boolean questObligatoire = questNode.path("obligatoire").asBoolean(true);

        Question question = questionRepository.findByQuestionnaireIdAndCode(questionnaire.getId(), questCode)
                .orElse(null);

        if (question == null) {
            question = new Question();
            question.setCode(questCode);
            question.setTexte(questTexte);
            question.setOrdre(questOrdre);
            question.setObligatoire(questObligatoire);
            question.setQuestionnaire(questionnaire);
            question = questionRepository.save(question);
        } else {
            if (!questTexte.equals(question.getTexte()) || question.getOrdre() != questOrdre) {
                question.setTexte(questTexte);
                question.setOrdre(questOrdre);
                question = questionRepository.save(question);
            }
        }

        JsonNode reponsesNode = questNode.path("reponses");
        if (reponsesNode.isArray()) {
            for (JsonNode repNode : reponsesNode) {
                seedReponse(question, repNode, categoriesByCode);
            }
        }
    }

    private void seedReponse(Question question, JsonNode repNode, Map<String, CategorieBesoin> categoriesByCode) {
        String repCode = repNode.path("code").asText();
        String repLibelle = repNode.path("libelle").asText();
        String repValeur = repNode.path("valeur").asText();

        Reponse reponse = reponseRepository.findByQuestionIdAndCode(question.getId(), repCode)
                .orElse(null);

        if (reponse == null) {
            reponse = new Reponse();
            reponse.setCode(repCode);
            reponse.setLibelle(repLibelle);
            reponse.setValeur(repValeur);
            reponse.setPoids(0); // Le poids métier est désormais dans PonderationOrientation
            reponse.setQuestion(question);
            reponse = reponseRepository.save(reponse);
        } else {
            if (!repLibelle.equals(reponse.getLibelle())) {
                reponse.setLibelle(repLibelle);
                reponse = reponseRepository.save(reponse);
            }
        }

        JsonNode ponderationsNode = repNode.path("ponderations");
        if (ponderationsNode.isArray()) {
            for (JsonNode pondNode : ponderationsNode) {
                String catCode = pondNode.path("categorieCode").asText();
                int poids = pondNode.path("poids").asInt();

                CategorieBesoin targetCat = categoriesByCode.get(catCode);
                if (targetCat != null) {
                    seedPonderation(reponse, targetCat, poids);
                } else {
                    log.warn("⚠️ Catégorie cible [{}] non trouvée pour la pondération de la réponse {}", catCode, repCode);
                }
            }
        }
    }

    private void seedPonderation(Reponse reponse, CategorieBesoin targetCat, int poids) {
        if (!ponderationOrientationRepository.existsByReponseIdAndCategorieBesoinId(reponse.getId(), targetCat.getId())) {
            PonderationOrientation pond = new PonderationOrientation();
            pond.setReponse(reponse);
            pond.setCategorieBesoin(targetCat);
            pond.setSpecialite(null);
            pond.setPoids(poids);
            ponderationOrientationRepository.save(pond);
        }
    }
}
