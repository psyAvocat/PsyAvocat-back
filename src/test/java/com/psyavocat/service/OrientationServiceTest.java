package com.psyavocat.service;

import com.psyavocat.dto.orientation.ResultatOrientationDTO;
import com.psyavocat.dto.orientation.SoumissionQuestionnaireRequest;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.PatientRepository;
import com.psyavocat.repository.QuestionnaireRepository;
import com.psyavocat.repository.ReponseRepository;
import com.psyavocat.repository.ResultatOrientationRepository;
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
    private ReponseRepository reponseRepository;

    @Mock
    private ResultatOrientationRepository resultatOrientationRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private CategorieBesoinRepository categorieBesoinRepository;

    @Mock
    private AuthenticationContext authenticationContext;

    private OrientationServiceImpl orientationService;

    private Patient patient;
    private Questionnaire questionnaire;

    @BeforeEach
    void setUp() {
        orientationService = new OrientationServiceImpl(
                questionnaireRepository,
                reponseRepository,
                resultatOrientationRepository,
                patientRepository,
                categorieBesoinRepository,
                authenticationContext
        );

        patient = new Patient();
        patient.setId("patient-1");
        patient.setNom("Dupont");

        questionnaire = new Questionnaire();
        questionnaire.setId("quest-1");
        questionnaire.setTitre("Bilan d'anxiété");
        questionnaire.setActif(true);
    }

    @Test
    @DisplayName("Évaluation du questionnaire : calcul du score cumulé et détermination de catégorie")
    void testEvaluerQuestionnaire_Succes() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(questionnaireRepository.findById(questionnaire.getId())).thenReturn(Optional.of(questionnaire));

        Reponse r1 = new Reponse();
        r1.setId("rep-1");
        r1.setPoids(5);

        Reponse r2 = new Reponse();
        r2.setId("rep-2");
        r2.setPoids(10);

        when(reponseRepository.findAllById(List.of("rep-1", "rep-2"))).thenReturn(List.of(r1, r2));

        CategorieBesoin cat = new CategorieBesoin();
        cat.setId("cat-1");
        cat.setNom("Anxiété et stress");
        when(categorieBesoinRepository.findByActifTrueAndTypeProfessionnel("PSYCHOLOGUE")).thenReturn(List.of(cat));

        when(resultatOrientationRepository.save(any(ResultatOrientation.class))).thenAnswer(i -> {
            ResultatOrientation res = i.getArgument(0);
            res.setId("res-1");
            return res;
        });

        SoumissionQuestionnaireRequest req = new SoumissionQuestionnaireRequest();
        req.setQuestionnaireId(questionnaire.getId());
        req.setReponseIds(List.of("rep-1", "rep-2"));

        ResultatOrientationDTO dto = orientationService.evaluerQuestionnaire(req);

        assertThat(dto).isNotNull();
        assertThat(dto.getScore()).isEqualTo(15.0);
        assertThat(dto.getCategorieBesoinNom()).isEqualTo("Anxiété et stress");

        verify(resultatOrientationRepository).save(any(ResultatOrientation.class));
    }

    @Test
    @DisplayName("Rejet si aucune réponse valide n'est fournie")
    void testEvaluerQuestionnaire_SansReponsesValides_BadRequest() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(patient.getId());
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(questionnaireRepository.findById(questionnaire.getId())).thenReturn(Optional.of(questionnaire));
        when(reponseRepository.findAllById(any())).thenReturn(List.of());

        SoumissionQuestionnaireRequest req = new SoumissionQuestionnaireRequest();
        req.setQuestionnaireId(questionnaire.getId());
        req.setReponseIds(List.of("rep-invalide"));

        assertThatThrownBy(() -> orientationService.evaluerQuestionnaire(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Aucune réponse valide trouvée");
    }
}
