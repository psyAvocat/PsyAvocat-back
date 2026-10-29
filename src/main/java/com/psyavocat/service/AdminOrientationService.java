package com.psyavocat.service;

import com.psyavocat.dto.orientation.PonderationCreateRequest;
import com.psyavocat.dto.orientation.PonderationDTO;
import com.psyavocat.dto.orientation.QuestionCreateRequest;
import com.psyavocat.dto.orientation.QuestionDTO;
import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.dto.orientation.ReponseCreateRequest;
import com.psyavocat.dto.orientation.ReponseDTO;

import java.util.List;

/**
 * Gestion de l'orientation depuis l'espace Admin : questions, réponses et pondérations du
 * questionnaire d'orientation psychologique (unique, créé au démarrage).
 */
public interface AdminOrientationService {

    // =========================================================================
    // QUESTIONNAIRES
    // =========================================================================

    /** Le questionnaire d'orientation psychologique. */
    QuestionnaireDTO getQuestionnaireOrientation();

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

    /** Active / désactive une question (désactivée : plus proposée aux nouveaux répondants). */
    QuestionDTO changerActivationQuestion(String id, boolean actif);

    /** Active / désactive une réponse. */
    ReponseDTO changerActivationReponse(String id, boolean actif);
}
