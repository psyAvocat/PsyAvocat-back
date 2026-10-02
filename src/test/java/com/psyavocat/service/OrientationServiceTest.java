package com.psyavocat.service;

import com.psyavocat.dto.orientation.ResultatOrientationDTO;
import com.psyavocat.dto.orientation.SoumissionQuestionnaireRequest;
import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.PonderationOrientation;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.Question;
import com.psyavocat.entity.Questionnaire;
import com.psyavocat.entity.Reponse;
import com.psyavocat.entity.ResultatOrientation;
import com.psyavocat.entity.Specialite;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.mapper.ProfessionnelMapper;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.PonderationOrientationRepository;
import com.psyavocat.repository.QuestionRepository;
import com.psyavocat.repository.QuestionnaireRepository;
import com.psyavocat.repository.ReponseRepository;
import com.psyavocat.repository.ResultatOrientationCategorieRepository;
import com.psyavocat.repository.ResultatOrientationRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.impl.OrientationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrientationServiceTest {

    @Mock
    private QuestionnaireRepository questionnaireRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ReponseRepository reponseRepository;

    @Mock
    private PonderationOrientationRepository ponderationOrientationRepository;

    @Mock
    private ResultatOrientationRepository resultatOrientationRepository;

    @Mock
    private ResultatOrientationCategorieRepository resultatOrientationCategorieRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private CategorieBesoinRepository categorieBesoinRepository;

    @Mock
    private SpecialiteRepository specialiteRepository;

    @Mock
    private MatchingProfessionnelService matchingProfessionnelService;

    @Mock
    private ProfessionnelMapper professionnelMapper;

    @Mock
    private AuthenticationContext authenticationContext;

    private OrientationServiceImpl orientationService;

    private Questionnaire questionnaire;
    private Question question1;
    private Question question2;

    @BeforeEach
    void setUp() {
        orientationService = new OrientationServiceImpl(
                questionnaireRepository,
                questionRepository,
                reponseRepository,
                ponderationOrientationRepository,
                resultatOrientationRepository,
                resultatOrientationCategorieRepository,
                utilisateurRepository,
                categorieBesoinRepository,
                specialiteRepository,
                matchingProfessionnelService,
                professionnelMapper,
                authenticationContext
        );

        questionnaire = new Questionnaire();
        questionnaire.setId("quest-psy-1");
        questionnaire.setCode("QUESTIONNAIRE_ORIENTATION_PSY_V1");
        questionnaire.setTitre("Questionnaire d’orientation psychologique");
        questionnaire.setType("PSYCHOLOGIQUE");
        questionnaire.setActif(true);

        question1 = new Question();
        question1.setId("q-1");
        question1.setCode("Q01");
        question1.setTexte("Quel est votre motif principal ?");
        question1.setOrdre(1);
        question1.setObligatoire(true);
        question1.setQuestionnaire(questionnaire);

        question2 = new Question();
        question2.setId("q-2");
        question2.setCode("Q02");
        question2.setTexte("Dans quel contexte apparaît votre difficulté ?");
        question2.setOrdre(2);
        question2.setObligatoire(true);
        question2.setQuestionnaire(questionnaire);

        questionnaire.setQuestions(List.of(question1, question2));
    }

    @Test
    @DisplayName("Calcul multi-catégories : la catégorie avec le score maximal l'emporte")
    void testEvaluerQuestionnaire_MultiCategories_MaxGagnant() {
        when(questionnaireRepository.findById(questionnaire.getId())).thenReturn(Optional.of(questionnaire));
        when(questionRepository.findByQuestionnaireIdOrderByOrdreAsc(questionnaire.getId()))
                .thenReturn(List.of(question1, question2));

        // Réponses
        Reponse r1 = new Reponse();
        r1.setId("r1");
        r1.setCode("Q01_R01");
        r1.setQuestion(question1);

        Reponse r2 = new Reponse();
        r2.setId("r2");
        r2.setCode("Q02_R01");
        r2.setQuestion(question2);

        when(reponseRepository.findAllById(List.of("r1", "r2"))).thenReturn(List.of(r1, r2));

        // Catégories
        CategorieBesoin catClinique = new CategorieBesoin();
        catClinique.setId("cat-clinique");
        catClinique.setCode("CLINIQUE");
        catClinique.setNom("Psychologie clinique et bien-être");

        CategorieBesoin catTravail = new CategorieBesoin();
        catTravail.setId("cat-travail");
        catTravail.setCode("TRAVAIL");
        catTravail.setNom("Psychologie du travail");

        // Pondérations issues de MySQL
        PonderationOrientation pond1 = new PonderationOrientation();
        pond1.setReponse(r1);
        pond1.setCategorieBesoin(catClinique);
        pond1.setPoids(10);

        PonderationOrientation pond2 = new PonderationOrientation();
        pond2.setReponse(r2);
        pond2.setCategorieBesoin(catClinique);
        pond2.setPoids(8);

        when(ponderationOrientationRepository.findByReponseIdIn(List.of("r1", "r2")))
                .thenReturn(List.of(pond1, pond2));

        Psychologue psy = new Psychologue();
        psy.setId("psy-1");
        psy.setNom("Lambert");
        when(matchingProfessionnelService.matcherParCategorieBesoin(catClinique, 5)).thenReturn(List.of(psy));

        when(resultatOrientationRepository.save(any(ResultatOrientation.class))).thenAnswer(i -> {
            ResultatOrientation res = i.getArgument(0);
            res.setId("res-123");
            return res;
        });

        SoumissionQuestionnaireRequest req = new SoumissionQuestionnaireRequest(questionnaire.getId(), List.of("r1", "r2"));
        ResultatOrientationDTO result = orientationService.evaluerQuestionnaire(req);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(18.0);
        assertThat(result.getCategorieBesoinCode()).isEqualTo("CLINIQUE");
        assertThat(result.getStatutDepartage()).isEqualTo("SCORE_UNIQUE_MAXIMAL");
        verify(resultatOrientationRepository).save(any(ResultatOrientation.class));
    }

    @Test
    @DisplayName("Départage d'égalité : la catégorie ciblée par Q01 départage les ex-aequo")
    void testEvaluerQuestionnaire_DepartageEgalite_ParQ01() {
        when(questionnaireRepository.findById(questionnaire.getId())).thenReturn(Optional.of(questionnaire));
        when(questionRepository.findByQuestionnaireIdOrderByOrdreAsc(questionnaire.getId()))
                .thenReturn(List.of(question1, question2));

        Reponse r1 = new Reponse();
        r1.setId("r1");
        r1.setCode("Q01_R01");
        r1.setQuestion(question1);

        Reponse r2 = new Reponse();
        r2.setId("r2");
        r2.setCode("Q02_R04");
        r2.setQuestion(question2);

        when(reponseRepository.findAllById(List.of("r1", "r2"))).thenReturn(List.of(r1, r2));

        CategorieBesoin catClinique = new CategorieBesoin();
        catClinique.setId("cat-clinique");
        catClinique.setCode("CLINIQUE");

        CategorieBesoin catTravail = new CategorieBesoin();
        catTravail.setId("cat-travail");
        catTravail.setCode("TRAVAIL");

        // Égalité : 10 points chacun
        PonderationOrientation pond1 = new PonderationOrientation();
        pond1.setReponse(r1);
        pond1.setCategorieBesoin(catClinique);
        pond1.setPoids(10);

        PonderationOrientation pond2 = new PonderationOrientation();
        pond2.setReponse(r2);
        pond2.setCategorieBesoin(catTravail);
        pond2.setPoids(10);

        when(ponderationOrientationRepository.findByReponseIdIn(List.of("r1", "r2")))
                .thenReturn(List.of(pond1, pond2));
        when(ponderationOrientationRepository.findByReponseId("r1"))
                .thenReturn(List.of(pond1));

        when(resultatOrientationRepository.save(any(ResultatOrientation.class))).thenAnswer(i -> i.getArgument(0));

        SoumissionQuestionnaireRequest req = new SoumissionQuestionnaireRequest(questionnaire.getId(), List.of("r1", "r2"));
        ResultatOrientationDTO result = orientationService.evaluerQuestionnaire(req);

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(10.0);
        // CLINIQUE est choisi car ciblé par Q01 !
        assertThat(result.getCategorieBesoinCode()).isEqualTo("CLINIQUE");
        assertThat(result.getStatutDepartage()).isEqualTo("EGALITE_DEPARTAGEE_PAR_Q01");
    }

    @Test
    @DisplayName("Validation : refus de sélection multiple pour une même question")
    void testEvaluerQuestionnaire_SelectionMultipleMemeQuestion_BadRequest() {
        when(questionnaireRepository.findById(questionnaire.getId())).thenReturn(Optional.of(questionnaire));

        Reponse r1_1 = new Reponse();
        r1_1.setId("r1_1");
        r1_1.setQuestion(question1);

        Reponse r1_2 = new Reponse();
        r1_2.setId("r1_2");
        r1_2.setQuestion(question1); // Deux réponses sur question1 !

        when(reponseRepository.findAllById(List.of("r1_1", "r1_2"))).thenReturn(List.of(r1_1, r1_2));

        SoumissionQuestionnaireRequest req = new SoumissionQuestionnaireRequest(questionnaire.getId(), List.of("r1_1", "r1_2"));

        assertThatThrownBy(() -> orientationService.evaluerQuestionnaire(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mode sélection unique");
    }

    @Test
    @DisplayName("Validation : refus si une question obligatoire n'a pas reçu de réponse")
    void testEvaluerQuestionnaire_QuestionObligatoireNonRepondue_BadRequest() {
        when(questionnaireRepository.findById(questionnaire.getId())).thenReturn(Optional.of(questionnaire));

        Reponse r1 = new Reponse();
        r1.setId("r1");
        r1.setQuestion(question1);

        // Seule la question1 a une réponse, pas question2 (qui est obligatoire)
        when(reponseRepository.findAllById(List.of("r1"))).thenReturn(List.of(r1));
        when(questionRepository.findByQuestionnaireIdOrderByOrdreAsc(questionnaire.getId()))
                .thenReturn(List.of(question1, question2));

        SoumissionQuestionnaireRequest req = new SoumissionQuestionnaireRequest(questionnaire.getId(), List.of("r1"));

        assertThatThrownBy(() -> orientationService.evaluerQuestionnaire(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'a pas reçu de réponse");
    }

    @Test
    @DisplayName("Validation : refus si le questionnaire est inactif")
    void testEvaluerQuestionnaire_QuestionnaireInactif_BadRequest() {
        questionnaire.setActif(false);
        when(questionnaireRepository.findById(questionnaire.getId())).thenReturn(Optional.of(questionnaire));

        SoumissionQuestionnaireRequest req = new SoumissionQuestionnaireRequest(questionnaire.getId(), List.of("r1"));

        assertThatThrownBy(() -> orientationService.evaluerQuestionnaire(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("n'est plus actif");
    }

    @Test
    @DisplayName("Validation : refus si le questionnaire n'existe pas")
    void testEvaluerQuestionnaire_QuestionnaireInexistant_NotFound() {
        when(questionnaireRepository.findById("inconnu")).thenReturn(Optional.empty());

        SoumissionQuestionnaireRequest req = new SoumissionQuestionnaireRequest("inconnu", List.of("r1"));

        assertThatThrownBy(() -> orientationService.evaluerQuestionnaire(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
