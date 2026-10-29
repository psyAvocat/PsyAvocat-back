package com.psyavocat.config;

import com.psyavocat.entity.Questionnaire;
import com.psyavocat.repository.QuestionnaireRepository;
import com.psyavocat.service.support.QuestionnaireOrientation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Initialisation du « Questionnaire d'orientation psychologique ».
 *
 * Idempotente : le questionnaire n'est créé qu'une seule fois (repéré par son code) ; aux
 * démarrages suivants rien n'est modifié. Si un questionnaire psychologique existe déjà
 * (créé auparavant depuis l'administration), il est repris tel quel — questions, réponses,
 * pondérations et résultats conservés — en privilégiant celui qui est publié, puis celui
 * qui compte le plus de questions. Les autres questionnaires psychologiques sont retirés
 * de la publication (leurs résultats passés restent consultables).
 */
@Slf4j
@Component
public class QuestionnaireOrientationInitializer implements ApplicationRunner {

    private final QuestionnaireRepository questionnaireRepository;

    public QuestionnaireOrientationInitializer(QuestionnaireRepository questionnaireRepository) {
        this.questionnaireRepository = questionnaireRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Questionnaire reference = questionnaireRepository.findByCode(QuestionnaireOrientation.CODE).orElse(null);
        if (reference == null) {
            reference = reprendreOuCreer();
        }

        // Un seul questionnaire d'orientation publié.
        for (Questionnaire autre : questionnaireRepository.findAllByTypeAndActifTrue(QuestionnaireOrientation.TYPE)) {
            if (!autre.getId().equals(reference.getId())) {
                autre.setActif(false);
                questionnaireRepository.save(autre);
                log.info("[Orientation] Questionnaire « {} » retiré de la publication.", autre.getTitre());
            }
        }
    }

    private Questionnaire reprendreOuCreer() {
        List<Questionnaire> psychologiques = questionnaireRepository.findAll().stream()
                .filter(q -> QuestionnaireOrientation.TYPE.equalsIgnoreCase(q.getType()))
                .toList();
        Questionnaire questionnaire = psychologiques.stream()
                .max(Comparator.comparing((Questionnaire q) -> Boolean.TRUE.equals(q.getActif()))
                        .thenComparing(q -> q.getQuestions() != null ? q.getQuestions().size() : 0))
                .orElse(null);

        if (questionnaire == null) {
            questionnaire = new Questionnaire();
            log.info("[Orientation] Création du « {} ».", QuestionnaireOrientation.TITRE);
        } else {
            log.info("[Orientation] Questionnaire existant « {} » repris comme questionnaire d'orientation psychologique.",
                    questionnaire.getTitre());
        }
        questionnaire.setCode(QuestionnaireOrientation.CODE);
        questionnaire.setTitre(QuestionnaireOrientation.TITRE);
        questionnaire.setType(QuestionnaireOrientation.TYPE);
        questionnaire.setActif(true);
        return questionnaireRepository.save(questionnaire);
    }
}
