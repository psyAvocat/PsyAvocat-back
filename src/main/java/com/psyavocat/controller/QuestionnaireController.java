package com.psyavocat.controller;

import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.service.OrientationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST dédié à l'exposition des questionnaires d'orientation (Juridique & Psychologique).
 */
@RestController
@RequestMapping("/api/questionnaires")
@RequiredArgsConstructor
public class QuestionnaireController {

    private final OrientationService orientationService;

    @GetMapping
    public ResponseEntity<List<QuestionnaireDTO>> getAllQuestionnaires(
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(orientationService.getQuestionnaires(type));
    }

    @GetMapping("/psychologique")
    public ResponseEntity<QuestionnaireDTO> getQuestionnairePsychologique() {
        return ResponseEntity.ok(orientationService.getQuestionnaireByType("PSYCHOLOGIQUE"));
    }

    @GetMapping("/juridique")
    public ResponseEntity<QuestionnaireDTO> getQuestionnaireJuridique() {
        return ResponseEntity.ok(orientationService.getQuestionnaireByType("JURIDIQUE"));
    }

    @GetMapping("/{type}")
    public ResponseEntity<QuestionnaireDTO> getQuestionnaireByType(@PathVariable String type) {
        return ResponseEntity.ok(orientationService.getQuestionnaireByType(type));
    }
}
