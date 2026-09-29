package com.psyavocat.controller;

import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.dto.orientation.ResultatOrientationDTO;
import com.psyavocat.dto.orientation.SoumissionQuestionnaireRequest;
import com.psyavocat.service.OrientationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orientation")
public class OrientationController {

    private final OrientationService orientationService;

    public OrientationController(OrientationService orientationService) {
        this.orientationService = orientationService;
    }

    @GetMapping("/questionnaires")
    public ResponseEntity<List<QuestionnaireDTO>> getQuestionnaires(
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(orientationService.getQuestionnaires(type));
    }

    @GetMapping("/questionnaires/type/{type}")
    public ResponseEntity<QuestionnaireDTO> getQuestionnaireByType(@PathVariable String type) {
        return ResponseEntity.ok(orientationService.getQuestionnaireByType(type));
    }

    @PostMapping("/evaluer")
    public ResponseEntity<ResultatOrientationDTO> evaluerQuestionnaire(
            @Valid @RequestBody SoumissionQuestionnaireRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orientationService.evaluerQuestionnaire(request));
    }

    @GetMapping("/mes-resultats")
    public ResponseEntity<List<ResultatOrientationDTO>> getMesResultats() {
        return ResponseEntity.ok(orientationService.getMesResultats());
    }
}
