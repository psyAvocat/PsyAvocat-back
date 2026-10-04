package com.psyavocat.controller;

import com.psyavocat.dto.orientation.PonderationCreateRequest;
import com.psyavocat.dto.orientation.PonderationDTO;
import com.psyavocat.dto.orientation.QuestionCompleteCreateRequest;
import com.psyavocat.dto.orientation.QuestionCreateRequest;
import com.psyavocat.dto.orientation.QuestionDTO;
import com.psyavocat.dto.orientation.QuestionnaireCreateRequest;
import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.dto.orientation.ReponseCreateRequest;
import com.psyavocat.dto.orientation.ReponseDTO;
import com.psyavocat.service.AdminOrientationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST Admin pour la gestion des questionnaires d'orientation.
 * Accessible uniquement aux administrateurs (rôle ADMINISTRATEUR).
 * Base URL : /api/admin/orientation
 */
@RestController
@RequestMapping("/api/admin/orientation")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@RequiredArgsConstructor
public class AdminOrientationController {

    private final AdminOrientationService adminOrientationService;

    // =========================================================================
    // QUESTIONNAIRES
    // =========================================================================

    @GetMapping("/questionnaires")
    public ResponseEntity<List<QuestionnaireDTO>> getAllQuestionnaires() {
        return ResponseEntity.ok(adminOrientationService.getAllQuestionnaires());
    }

    @GetMapping("/questionnaires/{id}")
    public ResponseEntity<QuestionnaireDTO> getQuestionnaire(@PathVariable String id) {
        return ResponseEntity.ok(adminOrientationService.getQuestionnaireById(id));
    }

    @PostMapping("/questionnaires")
    public ResponseEntity<QuestionnaireDTO> createQuestionnaire(
            @Valid @RequestBody QuestionnaireCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminOrientationService.createQuestionnaire(request));
    }

    @PutMapping("/questionnaires/{id}")
    public ResponseEntity<QuestionnaireDTO> updateQuestionnaire(
            @PathVariable String id,
            @Valid @RequestBody QuestionnaireCreateRequest request) {
        return ResponseEntity.ok(adminOrientationService.updateQuestionnaire(id, request));
    }

    @DeleteMapping("/questionnaires/{id}")
    public ResponseEntity<Void> deleteQuestionnaire(@PathVariable String id) {
        adminOrientationService.deleteQuestionnaire(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // QUESTIONS
    // =========================================================================

    @GetMapping("/questions")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByQuestionnaire(
            @RequestParam String questionnaireId) {
        return ResponseEntity.ok(adminOrientationService.getQuestionsByQuestionnaire(questionnaireId));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<QuestionDTO> getQuestion(@PathVariable String id) {
        return ResponseEntity.ok(adminOrientationService.getQuestionById(id));
    }

    @PostMapping("/questions")
    public ResponseEntity<QuestionDTO> createQuestion(
            @Valid @RequestBody QuestionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminOrientationService.createQuestion(request));
    }

    @PostMapping("/questions/complete")
    public ResponseEntity<QuestionDTO> createQuestionComplete(
            @Valid @RequestBody QuestionCompleteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminOrientationService.createQuestionComplete(request));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<QuestionDTO> updateQuestion(
            @PathVariable String id,
            @Valid @RequestBody QuestionCreateRequest request) {
        return ResponseEntity.ok(adminOrientationService.updateQuestion(id, request));
    }

    @PutMapping("/questions/{id}/complete")
    public ResponseEntity<QuestionDTO> updateQuestionComplete(
            @PathVariable String id,
            @Valid @RequestBody QuestionCompleteCreateRequest request) {
        return ResponseEntity.ok(adminOrientationService.updateQuestionComplete(id, request));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String id) {
        adminOrientationService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // RÉPONSES
    // =========================================================================

    @GetMapping("/reponses")
    public ResponseEntity<List<ReponseDTO>> getReponsesByQuestion(
            @RequestParam String questionId) {
        return ResponseEntity.ok(adminOrientationService.getReponsesByQuestion(questionId));
    }

    @GetMapping("/reponses/{id}")
    public ResponseEntity<ReponseDTO> getReponse(@PathVariable String id) {
        return ResponseEntity.ok(adminOrientationService.getReponseById(id));
    }

    @PostMapping("/reponses")
    public ResponseEntity<ReponseDTO> createReponse(
            @Valid @RequestBody ReponseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminOrientationService.createReponse(request));
    }

    @PutMapping("/reponses/{id}")
    public ResponseEntity<ReponseDTO> updateReponse(
            @PathVariable String id,
            @Valid @RequestBody ReponseCreateRequest request) {
        return ResponseEntity.ok(adminOrientationService.updateReponse(id, request));
    }

    @DeleteMapping("/reponses/{id}")
    public ResponseEntity<Void> deleteReponse(@PathVariable String id) {
        adminOrientationService.deleteReponse(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // PONDÉRATIONS
    // =========================================================================

    @GetMapping("/ponderations")
    public ResponseEntity<List<PonderationDTO>> getPonderationsByReponse(
            @RequestParam String reponseId) {
        return ResponseEntity.ok(adminOrientationService.getPonderationsByReponse(reponseId));
    }

    @PostMapping("/ponderations")
    public ResponseEntity<PonderationDTO> createPonderation(
            @Valid @RequestBody PonderationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminOrientationService.createPonderation(request));
    }

    @PutMapping("/ponderations/{id}")
    public ResponseEntity<PonderationDTO> updatePonderation(
            @PathVariable String id,
            @Valid @RequestBody PonderationCreateRequest request) {
        return ResponseEntity.ok(adminOrientationService.updatePonderation(id, request));
    }

    @DeleteMapping("/ponderations/{id}")
    public ResponseEntity<Void> deletePonderation(@PathVariable String id) {
        adminOrientationService.deletePonderation(id);
        return ResponseEntity.noContent().build();
    }
}
