package com.psyavocat.service.impl;

import com.psyavocat.dto.orientation.PonderationCreateRequest;
import com.psyavocat.dto.orientation.PonderationDTO;
import com.psyavocat.dto.orientation.QuestionCompleteCreateRequest;
import com.psyavocat.dto.orientation.QuestionCreateRequest;
import com.psyavocat.dto.orientation.QuestionDTO;
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
import com.psyavocat.service.support.QuestionnaireOrientation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** Le questionnaire d'orientation psychologique, créé au démarrage par QuestionnaireOrientationInitializer. */
    private Questionnaire questionnaireOrientation() {
        return questionnaireRepository.findByCode(QuestionnaireOrientation.CODE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Le questionnaire d'orientation psychologique n'est pas encore initialisé : redémarrez le serveur."));
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionnaireDTO getQuestionnaireOrientation() {
        return toQuestionnaireDTO(questionnaireOrientation());
    }

    @Override
    public QuestionDTO changerActivationQuestion(String id, boolean actif) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + id));
        q.setActif(actif);
        return toQuestionDTO(questionRepository.save(q));
    }

    @Override
    public ReponseDTO changerActivationReponse(String id, boolean actif) {
        Reponse r = reponseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réponse introuvable : " + id));
        r.setActif(actif);
        return toReponseDTO(reponseRepository.save(r));
    }

    // =========================================================================
    // QUESTIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> getQuestionsByQuestionnaire(String questionnaireId) {
        String id = StringUtils.hasText(questionnaireId) ? questionnaireId : questionnaireOrientation().getId();
        questionnaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + id));
        return questionRepository.findByQuestionnaireIdOrderByOrdreAsc(id)
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
        Questionnaire questionnaire = questionnaireOrientation();

        if (StringUtils.hasText(request.getCode()) &&
                questionRepository.existsByQuestionnaireIdAndCode(questionnaire.getId(), request.getCode())) {
            throw new BadRequestException("Une question avec ce code existe déjà dans ce questionnaire");
        }

        Question q = new Question();
        q.setTexte(request.getTexte().trim());
        q.setCode(StringUtils.hasText(request.getCode()) ? request.getCode().trim() : null);
        q.setOrdre(request.getOrdre());
        q.setObligatoire(request.getObligatoire() != null ? request.getObligatoire() : false);
        q.setContexte(StringUtils.hasText(request.getContexte()) ? request.getContexte().trim() : null);
        q.setTypeReponse(StringUtils.hasText(request.getTypeReponse()) ? request.getTypeReponse().trim() : "CHOIX_UNIQUE");
        q.setQuestionnaire(questionnaire);

        Question saved = questionRepository.save(q);
        log.info("Admin: question créée [{}] dans questionnaire [{}]", saved.getId(), questionnaire.getId());
        return toQuestionDTO(saved);
    }

    @Override
    public QuestionDTO createQuestionComplete(QuestionCompleteCreateRequest request) {
        Questionnaire questionnaire = questionnaireOrientation();

        if (StringUtils.hasText(request.getCode()) &&
                questionRepository.existsByQuestionnaireIdAndCode(questionnaire.getId(), request.getCode())) {
            throw new BadRequestException("Une question avec ce code existe déjà dans ce questionnaire");
        }

        Question q = new Question();
        q.setTexte(request.getTexte().trim());
        q.setCode(StringUtils.hasText(request.getCode()) ? request.getCode().trim() : null);
        q.setOrdre(request.getOrdre());
        q.setObligatoire(request.getObligatoire() != null ? request.getObligatoire() : false);
        q.setContexte(StringUtils.hasText(request.getContexte()) ? request.getContexte().trim() : null);
        q.setTypeReponse(StringUtils.hasText(request.getTypeReponse()) ? request.getTypeReponse().trim() : "CHOIX_UNIQUE");
        q.setQuestionnaire(questionnaire);

        Question savedQuestion = questionRepository.save(q);

        if (request.getReponses() != null && !request.getReponses().isEmpty()) {
            for (QuestionCompleteCreateRequest.ReponseCompleteItemRequest repReq : request.getReponses()) {
                if (!StringUtils.hasText(repReq.getLibelle())) continue;

                Reponse r = new Reponse();
                r.setLibelle(repReq.getLibelle().trim());
                r.setCode(StringUtils.hasText(repReq.getCode()) ? repReq.getCode().trim() : null);
                r.setValeur(repReq.getValeur());
                r.setQuestion(savedQuestion);
                Reponse savedRep = reponseRepository.save(r);

                if (repReq.getPonderations() != null && !repReq.getPonderations().isEmpty()) {
                    Set<String> seenCategories = new HashSet<>();
                    Set<String> seenSpecialites = new HashSet<>();

                    for (QuestionCompleteCreateRequest.PonderationItemRequest pondReq : repReq.getPonderations()) {
                        validerCiblePonderation(questionnaire.getType(), pondReq.getCategorieBesoinId(), pondReq.getSpecialiteId());

                        PonderationOrientation pond = new PonderationOrientation();
                        pond.setReponse(savedRep);
                        pond.setPoids(pondReq.getPoids() != null ? pondReq.getPoids() : 0);

                        if (StringUtils.hasText(pondReq.getCategorieBesoinId())) {
                            if (!seenCategories.add(pondReq.getCategorieBesoinId())) {
                                throw new BadRequestException("Une même catégorie ne peut pas être pondérée plusieurs fois pour la réponse « " + repReq.getLibelle() + " »");
                            }
                            CategorieBesoin cat = categorieBesoinRepository.findById(pondReq.getCategorieBesoinId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + pondReq.getCategorieBesoinId()));
                            pond.setCategorieBesoin(cat);
                        }

                        if (StringUtils.hasText(pondReq.getSpecialiteId())) {
                            if (!seenSpecialites.add(pondReq.getSpecialiteId())) {
                                throw new BadRequestException("Une même spécialité ne peut pas être pondérée plusieurs fois pour la réponse « " + repReq.getLibelle() + " »");
                            }
                            Specialite spec = specialiteRepository.findById(pondReq.getSpecialiteId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Spécialité introuvable : " + pondReq.getSpecialiteId()));
                            pond.setSpecialite(spec);
                        }

                        savedRep.getPonderations().add(ponderationOrientationRepository.save(pond));
                    }
                }
                savedQuestion.getReponses().add(savedRep);
            }
        }

        log.info("Admin: question complète créée [{}] avec {} réponses", savedQuestion.getId(), savedQuestion.getReponses().size());
        return toQuestionDTO(savedQuestion);
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
        if (request.getContexte() != null) {
            q.setContexte(StringUtils.hasText(request.getContexte()) ? request.getContexte().trim() : null);
        }
        if (StringUtils.hasText(request.getTypeReponse())) {
            q.setTypeReponse(request.getTypeReponse().trim());
        }
        // La question reste dans son questionnaire (pas de réaffectation).
        return toQuestionDTO(questionRepository.save(q));
    }

    @Override
    public QuestionDTO updateQuestionComplete(String id, QuestionCompleteCreateRequest request) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + id));

        // La question reste dans son questionnaire (pas de réaffectation).
        Questionnaire questionnaire = q.getQuestionnaire() != null ? q.getQuestionnaire() : questionnaireOrientation();
        q.setQuestionnaire(questionnaire);

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
        q.setContexte(StringUtils.hasText(request.getContexte()) ? request.getContexte().trim() : null);
        if (StringUtils.hasText(request.getTypeReponse())) {
            q.setTypeReponse(request.getTypeReponse().trim());
        }

        // État actif des réponses existantes, conservé lors de la reconstruction.
        java.util.Map<String, Boolean> actifParReponse = new java.util.HashMap<>();
        for (Reponse ancienne : q.getReponses()) {
            actifParReponse.put(ancienne.getId(), !Boolean.FALSE.equals(ancienne.getActif()));
        }

        // Vider les anciennes réponses pour reconstruire proprement la question
        q.getReponses().clear();
        questionRepository.saveAndFlush(q);

        if (request.getReponses() != null && !request.getReponses().isEmpty()) {
            for (QuestionCompleteCreateRequest.ReponseCompleteItemRequest repReq : request.getReponses()) {
                if (!StringUtils.hasText(repReq.getLibelle())) continue;

                Reponse r = new Reponse();
                r.setLibelle(repReq.getLibelle().trim());
                r.setCode(StringUtils.hasText(repReq.getCode()) ? repReq.getCode().trim() : null);
                r.setValeur(repReq.getValeur());
                r.setActif(repReq.getActif() != null ? repReq.getActif()
                        : actifParReponse.getOrDefault(repReq.getId() != null ? repReq.getId() : "", true));
                r.setQuestion(q);
                Reponse savedRep = reponseRepository.save(r);

                if (repReq.getPonderations() != null && !repReq.getPonderations().isEmpty()) {
                    Set<String> seenCategories = new HashSet<>();
                    Set<String> seenSpecialites = new HashSet<>();

                    for (QuestionCompleteCreateRequest.PonderationItemRequest pondReq : repReq.getPonderations()) {
                        validerCiblePonderation(questionnaire.getType(), pondReq.getCategorieBesoinId(), pondReq.getSpecialiteId());

                        PonderationOrientation pond = new PonderationOrientation();
                        pond.setReponse(savedRep);
                        pond.setPoids(pondReq.getPoids() != null ? pondReq.getPoids() : 0);

                        if (StringUtils.hasText(pondReq.getCategorieBesoinId())) {
                            if (!seenCategories.add(pondReq.getCategorieBesoinId())) {
                                throw new BadRequestException("Une même catégorie ne peut pas être pondérée plusieurs fois pour la réponse « " + repReq.getLibelle() + " »");
                            }
                            CategorieBesoin cat = categorieBesoinRepository.findById(pondReq.getCategorieBesoinId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + pondReq.getCategorieBesoinId()));
                            pond.setCategorieBesoin(cat);
                        }

                        if (StringUtils.hasText(pondReq.getSpecialiteId())) {
                            if (!seenSpecialites.add(pondReq.getSpecialiteId())) {
                                throw new BadRequestException("Une même spécialité ne peut pas être pondérée plusieurs fois pour la réponse « " + repReq.getLibelle() + " »");
                            }
                            Specialite spec = specialiteRepository.findById(pondReq.getSpecialiteId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Spécialité introuvable : " + pondReq.getSpecialiteId()));
                            pond.setSpecialite(spec);
                        }

                        savedRep.getPonderations().add(ponderationOrientationRepository.save(pond));
                    }
                }
                q.getReponses().add(savedRep);
            }
        }

        Question updated = questionRepository.save(q);
        log.info("Admin: question complète mise à jour [{}]", id);
        return toQuestionDTO(updated);
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
        Reponse reponse = reponseRepository.findById(request.getReponseId())
                .orElseThrow(() -> new ResourceNotFoundException("Réponse introuvable : " + request.getReponseId()));

        Questionnaire questionnaire = reponse.getQuestion() != null ? reponse.getQuestion().getQuestionnaire() : null;
        if (questionnaire != null) {
            validerCiblePonderation(questionnaire.getType(), request.getCategorieBesoinId(), request.getSpecialiteId());
        } else {
            if (!StringUtils.hasText(request.getCategorieBesoinId()) && !StringUtils.hasText(request.getSpecialiteId())) {
                throw new BadRequestException("Une pondération doit cibler soit une catégorie de besoin, soit une spécialité");
            }
        }

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

    private void validerCiblePonderation(String typeQuestionnaire, String categorieBesoinId, String specialiteId) {
        if (!StringUtils.hasText(categorieBesoinId) && !StringUtils.hasText(specialiteId)) {
            throw new BadRequestException("Une pondération doit obligatoirement avoir une cible (catégorie de besoin ou spécialité).");
        }
        if (StringUtils.hasText(categorieBesoinId) && StringUtils.hasText(specialiteId)) {
            throw new BadRequestException("Une pondération ne peut pas cibler simultanément une catégorie de besoin et une spécialité.");
        }
        if ("PSYCHOLOGIQUE".equalsIgnoreCase(typeQuestionnaire)) {
            if (!StringUtils.hasText(categorieBesoinId)) {
                throw new BadRequestException("Pour un questionnaire psychologique, la pondération doit obligatoirement cibler une catégorie de besoin.");
            }
            if (StringUtils.hasText(specialiteId)) {
                throw new BadRequestException("Pour un questionnaire psychologique, une spécialité juridique ne peut pas être ciblée.");
            }
            CategorieBesoin categorie = categorieBesoinRepository.findById(categorieBesoinId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + categorieBesoinId));
            if (!"PSYCHOLOGUE".equalsIgnoreCase(categorie.getTypeProfessionnel())) {
                throw new BadRequestException("La catégorie « " + categorie.getNom() + " » n'appartient pas à l'univers Psychologue.");
            }
        } else if ("JURIDIQUE".equalsIgnoreCase(typeQuestionnaire)) {
            if (!StringUtils.hasText(specialiteId)) {
                throw new BadRequestException("Pour un questionnaire juridique, la pondération doit obligatoirement cibler une spécialité.");
            }
            if (StringUtils.hasText(categorieBesoinId)) {
                throw new BadRequestException("Pour un questionnaire juridique, une catégorie de besoin ne peut pas être ciblée.");
            }
        } else if (typeQuestionnaire != null) {
            throw new BadRequestException("Type de questionnaire non supporté : " + typeQuestionnaire);
        }
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
                .contexte(q.getContexte())
                .typeReponse(q.getTypeReponse())
                .actif(!Boolean.FALSE.equals(q.getActif()))
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
                .actif(!Boolean.FALSE.equals(r.getActif()))
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
