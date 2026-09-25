package com.psyavocat.service;

import com.psyavocat.dto.orientation.*;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class OrientationService {

    private final QuestionnaireRepository questionnaireRepository;
    private final ReponseRepository reponseRepository;
    private final ResultatOrientationRepository resultatOrientationRepository;
    private final PatientRepository patientRepository;
    private final CategorieBesoinRepository categorieBesoinRepository;
    private final AuthenticationContext authenticationContext;

    public OrientationService(
            QuestionnaireRepository questionnaireRepository,
            ReponseRepository reponseRepository,
            ResultatOrientationRepository resultatOrientationRepository,
            PatientRepository patientRepository,
            CategorieBesoinRepository categorieBesoinRepository,
            AuthenticationContext authenticationContext
    ) {
        this.questionnaireRepository = questionnaireRepository;
        this.reponseRepository = reponseRepository;
        this.resultatOrientationRepository = resultatOrientationRepository;
        this.patientRepository = patientRepository;
        this.categorieBesoinRepository = categorieBesoinRepository;
        this.authenticationContext = authenticationContext;
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireDTO> getQuestionnaires() {
        return questionnaireRepository.findByActifTrue().stream()
                .map(this::toQuestionnaireDto)
                .toList();
    }

    public ResultatOrientationDTO evaluerQuestionnaire(SoumissionQuestionnaireRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Patient patient = patientRepository.findById(uid)
                .orElseThrow(() -> new ForbiddenException("Seul un patient peut passer un test d'orientation"));

        Questionnaire questionnaire = questionnaireRepository.findById(request.getQuestionnaireId())
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable"));

        List<Reponse> reponses = reponseRepository.findAllById(request.getReponseIds());
        if (reponses.isEmpty()) {
            throw new BadRequestException("Aucune réponse valide trouvée");
        }

        // Calcul du score basé sur les poids des réponses
        double scoreTotal = reponses.stream()
                .mapToInt(r -> r.getPoids() != null ? r.getPoids() : 0)
                .sum();

        // Sélection d'une catégorie de besoin pertinente pour l'orientation psychologique
        List<CategorieBesoin> categories = categorieBesoinRepository.findByActifTrueAndTypeProfessionnel("PSYCHOLOGUE");
        CategorieBesoin recommandee = categories.isEmpty() ? null : categories.get(0);

        ResultatOrientation resultat = new ResultatOrientation();
        resultat.setDateEvaluation(LocalDateTime.now());
        resultat.setScore(scoreTotal);
        resultat.setPatient(patient);
        resultat.setQuestionnaire(questionnaire);
        resultat.setCategorieBesoin(recommandee);

        ResultatOrientation saved = resultatOrientationRepository.save(resultat);
        return toResultatDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ResultatOrientationDTO> getMesResultats() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return resultatOrientationRepository.findByPatientIdOrderByDateEvaluationDesc(uid).stream()
                .map(this::toResultatDto)
                .toList();
    }

    private QuestionnaireDTO toQuestionnaireDto(Questionnaire q) {
        List<QuestionDTO> questionDtos = q.getQuestions() != null
                ? q.getQuestions().stream().map(this::toQuestionDto).toList()
                : Collections.emptyList();

        return QuestionnaireDTO.builder()
                .id(q.getId())
                .titre(q.getTitre())
                .type(q.getType())
                .actif(q.getActif())
                .questions(questionDtos)
                .build();
    }

    private QuestionDTO toQuestionDto(Question qu) {
        List<ReponseDTO> repDtos = qu.getReponses() != null
                ? qu.getReponses().stream()
                    .map(r -> new ReponseDTO(r.getId(), r.getLibelle(), r.getValeur(), r.getPoids()))
                    .toList()
                : Collections.emptyList();

        return QuestionDTO.builder()
                .id(qu.getId())
                .texte(qu.getTexte())
                .ordre(qu.getOrdre())
                .obligatoire(qu.getObligatoire())
                .reponses(repDtos)
                .build();
    }

    private ResultatOrientationDTO toResultatDto(ResultatOrientation r) {
        ResultatOrientationDTO.ResultatOrientationDTOBuilder builder = ResultatOrientationDTO.builder()
                .id(r.getId())
                .dateEvaluation(r.getDateEvaluation())
                .score(r.getScore());

        if (r.getQuestionnaire() != null) {
            builder.questionnaireId(r.getQuestionnaire().getId())
                    .questionnaireTitre(r.getQuestionnaire().getTitre());
        }

        if (r.getCategorieBesoin() != null) {
            builder.categorieBesoinId(r.getCategorieBesoin().getId())
                    .categorieBesoinNom(r.getCategorieBesoin().getNom())
                    .categorieBesoinDescription(r.getCategorieBesoin().getDescription());
        }

        return builder.build();
    }
}
