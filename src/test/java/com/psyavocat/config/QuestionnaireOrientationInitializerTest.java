package com.psyavocat.config;

import com.psyavocat.dto.orientation.QuestionCreateRequest;
import com.psyavocat.entity.Question;
import com.psyavocat.entity.Questionnaire;
import com.psyavocat.repository.*;
import com.psyavocat.service.impl.AdminOrientationServiceImpl;
import com.psyavocat.service.support.QuestionnaireOrientation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Questionnaire d'orientation psychologique : créé une seule fois, seul publié, reçoit toutes les questions. */
class QuestionnaireOrientationInitializerTest {

    private QuestionnaireRepository repository;
    private QuestionnaireOrientationInitializer initializer;
    private final Map<String, Questionnaire> base = new HashMap<>();
    private int sequence;

    @BeforeEach
    void setUp() {
        repository = mock(QuestionnaireRepository.class);
        initializer = new QuestionnaireOrientationInitializer(repository);
        when(repository.findAll()).thenAnswer(i -> new ArrayList<>(base.values()));
        when(repository.findByCode(any())).thenAnswer(i -> base.values().stream()
                .filter(q -> i.getArgument(0).equals(q.getCode())).findFirst());
        when(repository.findAllByTypeAndActifTrue(any())).thenAnswer(i -> base.values().stream()
                .filter(q -> i.getArgument(0).equals(q.getType()) && Boolean.TRUE.equals(q.getActif())).toList());
        when(repository.save(any(Questionnaire.class))).thenAnswer(i -> {
            Questionnaire q = i.getArgument(0);
            if (q.getId() == null) {
                q.setId("q-" + (++sequence));
            }
            base.put(q.getId(), q);
            return q;
        });
    }

    private Questionnaire questionnaire(String id, String titre, boolean actif, int nbQuestions) {
        Questionnaire q = new Questionnaire();
        q.setId(id);
        q.setTitre(titre);
        q.setType("PSYCHOLOGIQUE");
        q.setActif(actif);
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < nbQuestions; i++) {
            questions.add(new Question());
        }
        q.setQuestions(questions);
        base.put(id, q);
        return q;
    }

    @Test
    @DisplayName("Base vide : le questionnaire d'orientation psychologique est créé et publié")
    void creation() {
        initializer.run(null);

        Questionnaire q = repository.findByCode(QuestionnaireOrientation.CODE).orElseThrow();
        assertThat(q.getTitre()).isEqualTo("Questionnaire d'orientation psychologique");
        assertThat(q.getType()).isEqualTo("PSYCHOLOGIQUE");
        assertThat(q.getActif()).isTrue();
        assertThat(base).hasSize(1);
    }

    @Test
    @DisplayName("Questionnaire existant repris (questions conservées) ; les autres sont retirés de la publication")
    void repriseDeLExistant() {
        Questionnaire publie = questionnaire("ancien", "Orientation v2", true, 8);
        Questionnaire brouillon = questionnaire("brouillon", "Essai", false, 2);

        initializer.run(null);

        assertThat(publie.getCode()).isEqualTo(QuestionnaireOrientation.CODE);
        assertThat(publie.getQuestions()).hasSize(8);
        assertThat(brouillon.getCode()).isNull();
        assertThat(base).hasSize(2);
    }

    @Test
    @DisplayName("Démarrages suivants : rien n'est recréé")
    void idempotent() {
        initializer.run(null);
        clearInvocations(repository);

        initializer.run(null);

        verify(repository, never()).save(any());
        assertThat(base).hasSize(1);
    }

    @Test
    @DisplayName("Une nouvelle question est rattachée au questionnaire d'orientation, quel que soit l'identifiant envoyé")
    void questionRattacheeAuQuestionnaire() {
        initializer.run(null);
        Questionnaire reference = repository.findByCode(QuestionnaireOrientation.CODE).orElseThrow();
        QuestionRepository questions = mock(QuestionRepository.class);
        when(questions.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));
        AdminOrientationServiceImpl admin = new AdminOrientationServiceImpl(repository, questions,
                mock(ReponseRepository.class), mock(PonderationOrientationRepository.class),
                mock(CategorieBesoinRepository.class), mock(SpecialiteRepository.class));
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setTexte("Comment dormez-vous ?");
        request.setQuestionnaireId("un-autre-questionnaire");

        admin.createQuestion(request);

        verify(questions).save(argThat(q -> q.getQuestionnaire() == reference));
        assertThat(Optional.ofNullable(admin.getQuestionnaireOrientation().getTitre()))
                .contains("Questionnaire d'orientation psychologique");
    }
}
