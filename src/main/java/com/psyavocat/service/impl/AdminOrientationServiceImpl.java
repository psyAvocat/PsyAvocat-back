package com.psyavocat.service.impl;

import com.psyavocat.dto.orientation.PonderationCreateRequest;
import com.psyavocat.dto.orientation.PonderationDTO;
import com.psyavocat.dto.orientation.QuestionCreateRequest;
import com.psyavocat.dto.orientation.QuestionDTO;
import com.psyavocat.dto.orientation.QuestionnaireCreateRequest;
import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.dto.orientation.ReponseCreateRequest;
import com.psyavocat.dto.orientation.ReponseDTO;
import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.PonderationOrientation;
import com.psyavocat.entity.Question;
import com.psyavocat.entity.Questionnaire;
import com.psyavocat.entity.Reponse;
import com.psyavocat.entity.Specialite;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.PonderationOrientationRepository;
import com.psyavocat.repository.QuestionRepository;
import com.psyavocat.repository.QuestionnaireRepository;
import com.psyavocat.repository.ReponseRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.service.AdminOrientationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminOrientationServiceImpl implements AdminOrientationService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final ReponseRepository reponseRepository;
    private final PonderationOrientationRepository ponderationOrientationRepository;
    private final CategorieBesoinRepository categorieBesoinRepository;
    private final SpecialiteRepository specialiteRepository;

    // =========================================================================
    // QUESTIONNAIRES
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<QuestionnaireDTO> getAllQuestionnaires() {
        return questionnaireRepository.findAll().stream()
                .sorted(Comparator.comparing(Questionnaire::getTitre))
                .map(this::toQuestionnaireDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionnaireDTO getQuestionnaireById(String id) {
        Questionnaire q = questionnaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + id));
        return toQuestionnaireDTO(q);
    }

    @Override
    public QuestionnaireDTO createQuestionnaire(QuestionnaireCreateRequest request) {
        if (StringUtils.hasText(request.getCode()) && questionnaireRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Un questionnaire avec le code '" + request.getCode() + "' existe déjà");
        }
        Questionnaire q = new Questionnaire();
        q.setTitre(request.getTitre().trim());
        q.setCode(StringUtils.hasText(request.getCode()) ? request.getCode().trim().toUpperCase() : null);
        q.setType(request.getType().trim().toUpperCase());
        q.setActif(request.getActif() != null ? request.getActif() : true);
        Questionnaire saved = questionnaireRepository.save(q);
        log.info("Admin: questionnaire créé [{}] type={}", saved.getId(), saved.getType());
        return toQuestionnaireDTO(saved);
    }

    @Override
    public QuestionnaireDTO updateQuestionnaire(String id, QuestionnaireCreateRequest request) {
        Questionnaire q = questionnaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + id));
        q.setTitre(request.getTitre().trim());
        if (StringUtils.hasText(request.getCode())) {
            q.setCode(request.getCode().trim().toUpperCase());
        }
        q.setType(request.getType().trim().toUpperCase());
        if (request.getActif() != null) {
            q.setActif(request.getActif());
        }
        return toQuestionnaireDTO(questionnaireRepository.save(q));
    }

    @Override
    public void deleteQuestionnaire(String id) {
        Questionnaire q = questionnaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + id));
        log.info("Admin: suppression questionnaire [{}]", id);
        questionnaireRepository.delete(q);
    }

    // =========================================================================
    // QUESTIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> getQuestionsByQuestionnaire(String questionnaireId) {
        questionnaireRepository.findById(questionnaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + questionnaireId));
        return questionRepository.findByQuestionnaireIdOrderByOrdreAsc(questionnaireId)
                .stream()
                .map(this::toQuestionDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDTO getQuestionById(String id) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + id));
        return toQuestionDTO(q);
    }

    @Override
    public QuestionDTO createQuestion(QuestionCreateRequest request) {
        Questionnaire questionnaire = questionnaireRepository.findById(request.getQuestionnaireId())
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + request.getQuestionnaireId()));

        if (StringUtils.hasText(request.getCode()) &&
                questionRepository.existsByQuestionnaireIdAndCode(request.getQuestionnaireId(), request.getCode())) {
            throw new BadRequestException("Une question avec ce code existe déjà dans ce questionnaire");
        }

        Question q = new Question();
        q.setTexte(request.getTexte().trim());
        q.setCode(StringUtils.hasText(request.getCode()) ? request.getCode().trim() : null);
        q.setOrdre(request.getOrdre());
        q.setObligatoire(request.getObligatoire() != null ? request.getObligatoire() : false);
        q.setQuestionnaire(questionnaire);

        Question saved = questionRepository.save(q);
        log.info("Admin: question créée [{}] dans questionnaire [{}]", saved.getId(), request.getQuestionnaireId());
        return toQuestionDTO(saved);
    }

    @Override
    public QuestionDTO updateQuestion(String id, QuestionCreateRequest request) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + id));
        q.setTexte(request.getTexte().trim());
        if (StringUtils.hasText(request.getCode())) {
            q.setCode(request.getCode().trim());
        }
        if (request.getOrdre() != null) {
            q.setOrdre(request.getOrdre());
        }
        if (request.getObligatoire() != null) {
            q.setObligatoire(request.getObligatoire());
        }
        // Réaffectation à un autre questionnaire si demandé
        if (StringUtils.hasText(request.getQuestionnaireId())) {
            Questionnaire questionnaire = questionnaireRepository.findById(request.getQuestionnaireId())
                    .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + request.getQuestionnaireId()));
            q.setQuestionnaire(questionnaire);
        }
        return toQuestionDTO(questionRepository.save(q));
    }

    @Override
    public void deleteQuestion(String id) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + id));
        log.info("Admin: suppression question [{}]", id);
        questionRepository.delete(q);
    }

    // =========================================================================
    // RÉPONSES
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ReponseDTO> getReponsesByQuestion(String questionId) {
        questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + questionId));
        return reponseRepository.findAll().stream()
                .filter(r -> r.getQuestion() != null && questionId.equals(r.getQuestion().getId()))
                .sorted(Comparator.comparing(r -> r.getCode() != null ? r.getCode() : r.getLibelle()))
                .map(this::toReponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReponseDTO getReponseById(String id) {
        Reponse r = reponseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réponse introuvable : " + id));
        return toReponseDTO(r);
    }

    @Override
    public ReponseDTO createReponse(ReponseCreateRequest request) {
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + request.getQuestionId()));

        if (StringUtils.hasText(request.getCode()) &&
                reponseRepository.existsByQuestionIdAndCode(request.getQuestionId(), request.getCode())) {
            throw new BadRequestException("Une réponse avec ce code existe déjà pour cette question");
        }

        Reponse r = new Reponse();
        r.setLibelle(request.getLibelle().trim());
        r.setCode(StringUtils.hasText(request.getCode()) ? request.getCode().trim() : null);
        r.setValeur(request.getValeur());
        r.setQuestion(question); // ← relation persistée automatiquement

        Reponse saved = reponseRepository.save(r);
        log.info("Admin: réponse créée [{}] pour question [{}]", saved.getId(), request.getQuestionId());
        return toReponseDTO(saved);
    }

    @Override
    public ReponseDTO updateReponse(String id, ReponseCreateRequest request) {
        Reponse r = reponseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réponse introuvable : " + id));
        r.setLibelle(request.getLibelle().trim());
        if (StringUtils.hasText(request.getCode())) {
            r.setCode(request.getCode().trim());
        }
        r.setValeur(request.getValeur());
        // Réaffectation à une autre question si demandé
        if (StringUtils.hasText(request.getQuestionId())) {
            Question question = questionRepository.findById(request.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + request.getQuestionId()));
            r.setQuestion(question);
        }
        return toReponseDTO(reponseRepository.save(r));
    }

    @Override
    public void deleteReponse(String id) {
        Reponse r = reponseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réponse introuvable : " + id));
        log.info("Admin: suppression réponse [{}]", id);
        reponseRepository.delete(r);
    }

    // =========================================================================
    // PONDÉRATIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<PonderationDTO> getPonderationsByReponse(String reponseId) {
        return ponderationOrientationRepository.findByReponseId(reponseId)
                .stream()
                .map(this::toPonderationDTO)
                .toList();
    }

    @Override
    public PonderationDTO createPonderation(PonderationCreateRequest request) {
        if (!StringUtils.hasText(request.getCategorieBesoinId()) && !StringUtils.hasText(request.getSpecialiteId())) {
            throw new BadRequestException("Une pondération doit cibler soit une catégorie de besoin, soit une spécialité");
        }

        Reponse reponse = reponseRepository.findById(request.getReponseId())
                .orElseThrow(() -> new ResourceNotFoundException("Réponse introuvable : " + request.getReponseId()));

        PonderationOrientation p = new PonderationOrientation();
        p.setReponse(reponse);
        p.setPoids(request.getPoids());

        if (StringUtils.hasText(request.getCategorieBesoinId())) {
            if (ponderationOrientationRepository.existsByReponseIdAndCategorieBesoinId(
                    request.getReponseId(), request.getCategorieBesoinId())) {
                throw new BadRequestException("Une pondération pour cette réponse et cette catégorie existe déjà");
            }
            CategorieBesoin cat = categorieBesoinRepository.findById(request.getCategorieBesoinId())
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + request.getCategorieBesoinId()));
            p.setCategorieBesoin(cat);
        }

        if (StringUtils.hasText(request.getSpecialiteId())) {
            if (ponderationOrientationRepository.existsByReponseIdAndSpecialiteId(
                    request.getReponseId(), request.getSpecialiteId())) {
                throw new BadRequestException("Une pondération pour cette réponse et cette spécialité existe déjà");
            }
            Specialite spec = specialiteRepository.findById(request.getSpecialiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Spécialité introuvable : " + request.getSpecialiteId()));
            p.setSpecialite(spec);
        }

        PonderationOrientation saved = ponderationOrientationRepository.save(p);
        log.info("Admin: pondération créée [{}] reponse=[{}] poids={}", saved.getId(), request.getReponseId(), request.getPoids());
        return toPonderationDTO(saved);
    }

    @Override
    public PonderationDTO updatePonderation(String id, PonderationCreateRequest request) {
        PonderationOrientation p = ponderationOrientationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pondération introuvable : " + id));
        p.setPoids(request.getPoids());
        return toPonderationDTO(ponderationOrientationRepository.save(p));
    }

    @Override
    public void deletePonderation(String id) {
        PonderationOrientation p = ponderationOrientationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pondération introuvable : " + id));
        log.info("Admin: suppression pondération [{}]", id);
        ponderationOrientationRepository.delete(p);
    }

    // =========================================================================
    // MAPPERS INTERNES
    // =========================================================================

    private QuestionnaireDTO toQuestionnaireDTO(Questionnaire q) {
        List<QuestionDTO> questionsDto;
        if (q.getQuestions() != null) {
            questionsDto = q.getQuestions().stream()
                    .sorted(Comparator.comparingInt(quest -> quest.getOrdre() != null ? quest.getOrdre() : 0))
                    .map(this::toQuestionDTO)
                    .toList();
        } else {
            questionsDto = List.of();
        }

        return QuestionnaireDTO.builder()
                .id(q.getId())
                .code(q.getCode())
                .titre(q.getTitre())
                .type(q.getType())
                .actif(q.getActif())
                .questions(questionsDto)
                .build();
    }

    private QuestionDTO toQuestionDTO(Question q) {
        List<com.psyavocat.dto.orientation.ReponseDTO> reponsesDto;
        if (q.getReponses() != null) {
            reponsesDto = q.getReponses().stream()
                    .map(this::toReponseDTO)
                    .toList();
        } else {
            reponsesDto = List.of();
        }

        return QuestionDTO.builder()
                .id(q.getId())
                .code(q.getCode())
                .texte(q.getTexte())
                .ordre(q.getOrdre())
                .obligatoire(q.getObligatoire())
                .reponses(reponsesDto)
                .build();
    }

    private ReponseDTO toReponseDTO(Reponse r) {
        List<PonderationDTO> ponderationsDto;
        if (r.getPonderations() != null) {
            ponderationsDto = r.getPonderations().stream()
                    .map(this::toPonderationDTO)
                    .toList();
        } else {
            ponderationsDto = List.of();
        }

        return ReponseDTO.builder()
                .id(r.getId())
                .code(r.getCode())
                .libelle(r.getLibelle())
                .valeur(r.getValeur())
                .poids(r.getPoids())
                .questionId(r.getQuestion() != null ? r.getQuestion().getId() : null)
                .ponderations(ponderationsDto)
                .build();
    }

    private PonderationDTO toPonderationDTO(PonderationOrientation p) {
        return PonderationDTO.builder()
                .id(p.getId())
                .reponseId(p.getReponse() != null ? p.getReponse().getId() : null)
                .reponseLibelle(p.getReponse() != null ? p.getReponse().getLibelle() : null)
                .categorieBesoinId(p.getCategorieBesoin() != null ? p.getCategorieBesoin().getId() : null)
                .categorieBesoinNom(p.getCategorieBesoin() != null ? p.getCategorieBesoin().getNom() : null)
                .specialiteId(p.getSpecialite() != null ? p.getSpecialite().getId() : null)
                .specialiteNom(p.getSpecialite() != null ? p.getSpecialite().getNom() : null)
                .poids(p.getPoids())
                .build();
    }
}
