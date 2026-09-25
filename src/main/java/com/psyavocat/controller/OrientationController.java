package com.psyavocat.controller;

import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.dto.orientation.ResultatOrientationDTO;
import com.psyavocat.dto.orientation.SoumissionQuestionnaireRequest;
import com.psyavocat.service.OrientationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orientation")
public class OrientationController {

    private final OrientationService orientationService;

    public OrientationController(OrientationService orientationService) {
        this.orientationService = orientationService;
    }

    @GetMapping("/questionnaires")
    public ResponseEntity<List<QuestionnaireDTO>> getQuestionnaires() {
        return ResponseEntity.ok(orientationService.getQuestionnaires());
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
