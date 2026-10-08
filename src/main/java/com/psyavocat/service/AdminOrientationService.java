package com.psyavocat.service;

import com.psyavocat.dto.orientation.PonderationCreateRequest;
import com.psyavocat.dto.orientation.PonderationDTO;
import com.psyavocat.dto.orientation.QuestionCreateRequest;
import com.psyavocat.dto.orientation.QuestionDTO;
import com.psyavocat.dto.orientation.QuestionnaireCreateRequest;
import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.dto.orientation.ReponseCreateRequest;
import com.psyavocat.dto.orientation.ReponseDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion CRUD de l'orientation depuis l'espace Admin.
 * Couvre : Questionnaires, Questions, Réponses, Pondérations.
 */
public interface AdminOrientationService {

    // =========================================================================
    // QUESTIONNAIRES
    // =========================================================================

    List<QuestionnaireDTO> getAllQuestionnaires();

    QuestionnaireDTO getQuestionnaireById(String id);

    QuestionnaireDTO createQuestionnaire(QuestionnaireCreateRequest request);

    QuestionnaireDTO updateQuestionnaire(String id, QuestionnaireCreateRequest request);

    void deleteQuestionnaire(String id);

    // =========================================================================
    // QUESTIONS
    // =========================================================================

    List<QuestionDTO> getQuestionsByQuestionnaire(String questionnaireId);

    QuestionDTO getQuestionById(String id);

    QuestionDTO createQuestion(QuestionCreateRequest request);

    QuestionDTO createQuestionComplete(com.psyavocat.dto.orientation.QuestionCompleteCreateRequest request);

    QuestionDTO updateQuestion(String id, QuestionCreateRequest request);

    QuestionDTO updateQuestionComplete(String id, com.psyavocat.dto.orientation.QuestionCompleteCreateRequest request);

    void deleteQuestion(String id);

    // =========================================================================
    // RÉPONSES
    // =========================================================================

    List<ReponseDTO> getReponsesByQuestion(String questionId);

    ReponseDTO getReponseById(String id);

    ReponseDTO createReponse(ReponseCreateRequest request);

    ReponseDTO updateReponse(String id, ReponseCreateRequest request);

    void deleteReponse(String id);

    // =========================================================================
    // PONDÉRATIONS
    // =========================================================================

    List<PonderationDTO> getPonderationsByReponse(String reponseId);

    PonderationDTO createPonderation(PonderationCreateRequest request);

    PonderationDTO updatePonderation(String id, PonderationCreateRequest request);

    void deletePonderation(String id);
}
