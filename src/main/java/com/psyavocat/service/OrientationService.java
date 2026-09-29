package com.psyavocat.service;

import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.dto.orientation.ResultatOrientationDTO;
import com.psyavocat.dto.orientation.SoumissionQuestionnaireRequest;

import java.util.List;

/**
 * Contrat de service pour l'orientation et l'évaluation des questionnaires patients.
 */
public interface OrientationService {

    List<QuestionnaireDTO> getQuestionnaires(String type);

    QuestionnaireDTO getQuestionnaireByType(String type);

    ResultatOrientationDTO evaluerQuestionnaire(SoumissionQuestionnaireRequest request);

    List<ResultatOrientationDTO> getMesResultats();
}
