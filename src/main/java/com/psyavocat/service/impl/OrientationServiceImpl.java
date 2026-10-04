package com.psyavocat.service.impl;

import com.psyavocat.dto.orientation.QuestionDTO;
import com.psyavocat.dto.orientation.QuestionnaireDTO;
import com.psyavocat.dto.orientation.ReponseDTO;
import com.psyavocat.dto.orientation.ResultatOrientationCategorieDTO;
import com.psyavocat.dto.orientation.ResultatOrientationDTO;
import com.psyavocat.dto.orientation.SoumissionQuestionnaireRequest;
import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;
import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.PonderationOrientation;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Question;
import com.psyavocat.entity.Questionnaire;
import com.psyavocat.entity.Reponse;
import com.psyavocat.entity.ResultatOrientation;
import com.psyavocat.entity.ResultatOrientationCategorie;
import com.psyavocat.entity.Specialite;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.mapper.ProfessionnelMapper;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.PonderationOrientationRepository;
import com.psyavocat.repository.QuestionRepository;
import com.psyavocat.repository.QuestionnaireRepository;
import com.psyavocat.repository.ReponseRepository;
import com.psyavocat.repository.ResultatOrientationCategorieRepository;
import com.psyavocat.repository.ResultatOrientationRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.MatchingProfessionnelService;
import com.psyavocat.service.OrientationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implémentation du service d'orientation de PsyAvocat.
 *
 * Principes stricts :
 * 1. POO / SOLID : Découplage clair des responsabilités (Matching délégué à MatchingProfessionnelService).
 * 2. 100% Data-Driven : AUCUN score, AUCUNE question ni règle métier en dur dans le code.
 *    Les pondérations multi-critères proviennent exclusivement de la base MySQL (PonderationOrientation).
 * 3. Validation exhaustive (mode sélection unique, questions obligatoires, intégrité du questionnaire).
 * 4. Stratégie de départage déterministe en cas d'égalité (priorité au motif principal Q01).
 */
@Slf4j
@Service
@Transactional
public class OrientationServiceImpl implements OrientationService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final ReponseRepository reponseRepository;
    private final PonderationOrientationRepository ponderationOrientationRepository;
    private final ResultatOrientationRepository resultatOrientationRepository;
    private final ResultatOrientationCategorieRepository resultatCategorieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CategorieBesoinRepository categorieBesoinRepository;
    private final SpecialiteRepository specialiteRepository;
    private final MatchingProfessionnelService matchingProfessionnelService;
    private final ProfessionnelMapper professionnelMapper;
    private final AuthenticationContext authenticationContext;

    public OrientationServiceImpl(
            QuestionnaireRepository questionnaireRepository,
            QuestionRepository questionRepository,
            ReponseRepository reponseRepository,
            PonderationOrientationRepository ponderationOrientationRepository,
            ResultatOrientationRepository resultatOrientationRepository,
            ResultatOrientationCategorieRepository resultatCategorieRepository,
            UtilisateurRepository utilisateurRepository,
            CategorieBesoinRepository categorieBesoinRepository,
            SpecialiteRepository specialiteRepository,
            MatchingProfessionnelService matchingProfessionnelService,
            ProfessionnelMapper professionnelMapper,
            AuthenticationContext authenticationContext
    ) {
        this.questionnaireRepository = questionnaireRepository;
        this.questionRepository = questionRepository;
        this.reponseRepository = reponseRepository;
        this.ponderationOrientationRepository = ponderationOrientationRepository;
        this.resultatOrientationRepository = resultatOrientationRepository;
        this.resultatCategorieRepository = resultatCategorieRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.categorieBesoinRepository = categorieBesoinRepository;
        this.specialiteRepository = specialiteRepository;
        this.matchingProfessionnelService = matchingProfessionnelService;
        this.professionnelMapper = professionnelMapper;
        this.authenticationContext = authenticationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionnaireDTO> getQuestionnaires(String type) {
        List<Questionnaire> list = questionnaireRepository.findByActifTrue();
        if (type != null && !type.isBlank()) {
            list = list.stream()
                    .filter(q -> type.equalsIgnoreCase(q.getType()))
                    .toList();
        }
        return list.stream()
                .map(this::toQuestionnaireDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionnaireDTO getQuestionnaireByType(String type) {
        Questionnaire q = questionnaireRepository.findByCode(type)
                .orElseGet(() -> questionnaireRepository.findByTypeAndActifTrue(type.toUpperCase())
                        .orElseGet(() -> questionnaireRepository.findByType(type.toUpperCase())
                                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable pour le type/code : " + type))));
        return toQuestionnaireDto(q);
    }

    @Override
    public ResultatOrientationDTO evaluerQuestionnaire(SoumissionQuestionnaireRequest request) {
        // 1 & 2. Récupérer le questionnaire et vérifier son existence
        Questionnaire questionnaire = questionnaireRepository.findById(request.getQuestionnaireId())
                .orElseGet(() -> questionnaireRepository.findByCode(request.getQuestionnaireId())
                        .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + request.getQuestionnaireId())));

        // 3. Vérifier que le questionnaire est actif
        if (Boolean.FALSE.equals(questionnaire.getActif())) {
            throw new BadRequestException("Le questionnaire sélectionné n'est plus actif");
        }

        // 4. Vérifier et récupérer les réponses
        if (request.getReponseIds() == null || request.getReponseIds().isEmpty()) {
            throw new BadRequestException("La liste des réponses soumises ne peut pas être vide");
        }

        List<Reponse> reponses = reponseRepository.findAllById(request.getReponseIds());
        if (reponses.size() != request.getReponseIds().size()) {
            throw new BadRequestException("Certaines réponses sélectionnées n'existent pas en base de données");
        }

        // 5. Vérifier que chaque réponse appartient bien au questionnaire
        for (Reponse r : reponses) {
            if (r.getQuestion() == null || r.getQuestion().getQuestionnaire() == null ||
                    !r.getQuestion().getQuestionnaire().getId().equals(questionnaire.getId())) {
                throw new BadRequestException("La réponse [" + r.getId() + "] n'appartient pas au questionnaire sélectionné");
            }
        }

        // 6. Validation du mode de sélection par question
        Map<String, List<Reponse>> reponsesParQuestion = reponses.stream()
                .collect(Collectors.groupingBy(r -> r.getQuestion().getId()));

        for (Map.Entry<String, List<Reponse>> entry : reponsesParQuestion.entrySet()) {
            if (entry.getValue().size() > 1) {
                Reponse firstRep = entry.getValue().get(0);
                Question questionAssociee = firstRep.getQuestion();
                boolean allowsMultiple = questionAssociee != null &&
                        "CHOIX_MULTIPLE".equalsIgnoreCase(questionAssociee.getTypeReponse());
                if (!allowsMultiple) {
                    throw new BadRequestException("Mode sélection unique : une seule réponse est autorisée par question (question : "
                            + (questionAssociee != null ? questionAssociee.getTexte() : entry.getKey()) + ")");
                }
            }
        }

        // 6bis. Vérifier les questions obligatoires
        List<Question> questionsDuQuestionnaire = questionRepository.findByQuestionnaireIdOrderByOrdreAsc(questionnaire.getId());
        for (Question q : questionsDuQuestionnaire) {
            if (Boolean.TRUE.equals(q.getObligatoire()) && !reponsesParQuestion.containsKey(q.getId())) {
                throw new BadRequestException("La question obligatoire '" + q.getTexte() + "' n'a pas reçu de réponse");
            }
        }

        // 7. Récupérer toutes les pondérations associées aux réponses
        List<String> reponseIds = reponses.stream().map(Reponse::getId).toList();
        List<PonderationOrientation> ponderations = ponderationOrientationRepository.findByReponseIdIn(reponseIds);

        CategorieBesoin categorieGagnante = null;
        Specialite specialiteGagnante = null;
        double scoreMaximal = 0.0;
        String statutDepartage = "SCORE_UNIQUE_MAXIMAL";

        if ("PSYCHOLOGIQUE".equalsIgnoreCase(questionnaire.getType())) {
            // 8 & 9. Agréger les poids par CategorieBesoin
            Map<CategorieBesoin, Integer> scoresParCat = new HashMap<>();
            for (PonderationOrientation p : ponderations) {
                if (p.getCategorieBesoin() != null) {
                    scoresParCat.merge(p.getCategorieBesoin(), p.getPoids(), Integer::sum);
                }
            }

            if (scoresParCat.isEmpty()) {
                throw new BadRequestException("Aucune pondération trouvée en base pour orienter les réponses fournies");
            }

            // 10. Déterminer la cible ayant le score maximal
            int maxScore = Collections.max(scoresParCat.values());
            scoreMaximal = (double) maxScore;

            List<CategorieBesoin> candidatesTied = scoresParCat.entrySet().stream()
                    .filter(e -> e.getValue() == maxScore)
                    .map(Map.Entry::getKey)
                    .toList();

            if (candidatesTied.size() == 1) {
                categorieGagnante = candidatesTied.get(0);
            } else {
                // 13. Stratégie d'égalité : départage par la question Q01 (motif principal)
                statutDepartage = "EGALITE_DEPARTAGEE_PAR_Q01";
                Reponse reponseQ01 = trouverReponseQuestionOrdre(reponses, 1, "Q01");

                CategorieBesoin catQ01 = null;
                if (reponseQ01 != null) {
                    List<PonderationOrientation> pondsQ01 = ponderationOrientationRepository.findByReponseId(reponseQ01.getId());
                    if (!pondsQ01.isEmpty()) {
                        catQ01 = pondsQ01.get(0).getCategorieBesoin();
                    }
                }

                if (catQ01 != null && candidatesTied.contains(catQ01)) {
                    categorieGagnante = catQ01;
                    log.info("ℹ️ Égalité de score maximal ({}) départagée en faveur de la catégorie Q01 [{}]", maxScore, catQ01.getCode());
                } else {
                    // Si Q01 ne cible aucune des catégories ex-aequo, départage déterministe documenté
                    statutDepartage = "EGALITE_DEPARTAGEE_DETERMINISTE";
                    categorieGagnante = candidatesTied.stream()
                            .min(Comparator.comparing(c -> c.getCode() != null ? c.getCode() : c.getNom()))
                            .orElse(candidatesTied.get(0));
                    log.warn("⚠️ Ambiguïté persistante : catégorie retenue par ordre déterministe [{}]", categorieGagnante.getCode());
                }
            }

        } else if ("JURIDIQUE".equalsIgnoreCase(questionnaire.getType())) {
            // Cas JURIDIQUE : Regroupement par Spécialité
            Map<Specialite, Integer> scoresParSpec = new HashMap<>();
            for (PonderationOrientation p : ponderations) {
                if (p.getSpecialite() != null) {
                    scoresParSpec.merge(p.getSpecialite(), p.getPoids(), Integer::sum);
                }
            }

            if (scoresParSpec.isEmpty()) {
                throw new BadRequestException("Aucune pondération trouvée en base pour orienter les réponses juridiques");
            }

            int maxScore = Collections.max(scoresParSpec.values());
            scoreMaximal = (double) maxScore;

            List<Specialite> candidatesTied = scoresParSpec.entrySet().stream()
                    .filter(e -> e.getValue() == maxScore)
                    .map(Map.Entry::getKey)
                    .toList();

            if (candidatesTied.size() == 1) {
                specialiteGagnante = candidatesTied.get(0);
            } else {
                statutDepartage = "EGALITE_DEPARTAGEE_PAR_Q01";
                Reponse reponseQ01 = trouverReponseQuestionOrdre(reponses, 1, "Q01");
                Specialite specQ01 = null;
                if (reponseQ01 != null) {
                    List<PonderationOrientation> pondsQ01 = ponderationOrientationRepository.findByReponseId(reponseQ01.getId());
                    if (!pondsQ01.isEmpty()) {
                        specQ01 = pondsQ01.get(0).getSpecialite();
                    }
                }
                if (specQ01 != null && candidatesTied.contains(specQ01)) {
                    specialiteGagnante = specQ01;
                } else {
                    statutDepartage = "EGALITE_DEPARTAGEE_DETERMINISTE";
                    specialiteGagnante = candidatesTied.stream()
                            .min(Comparator.comparing(Specialite::getNom))
                            .orElse(candidatesTied.get(0));
                }
            }
        } else {
            throw new BadRequestException("Type de questionnaire non supporté : " + questionnaire.getType());
        }

        // 13. Matching des professionnels (délégué au MatchingProfessionnelService)
        List<Professionnel> prosRecommandes;
        if (categorieGagnante != null) {
            prosRecommandes = matchingProfessionnelService.matcherParCategorieBesoin(categorieGagnante, 5);
        } else {
            prosRecommandes = matchingProfessionnelService.matcherParSpecialite(specialiteGagnante, 5);
        }

        // 11. Créer ResultatOrientation
        Utilisateur user = obtenirUtilisateurCourant();

        ResultatOrientation resultat = new ResultatOrientation();
        resultat.setDateEvaluation(LocalDateTime.now());
        resultat.setScore(scoreMaximal);
        resultat.setQuestionnaire(questionnaire);
        resultat.setCategorieBesoin(categorieGagnante);
        resultat.setSpecialite(specialiteGagnante);
        resultat.setProfessionnelsRecommandes(prosRecommandes);
        resultat.setUtilisateur(user);

        // 12. Enregistrer le résultat si l'utilisateur est authentifié
        if (user != null) {
            resultat = resultatOrientationRepository.save(resultat);

            // 12bis. Persister le classement complet multi-catégories (uniquement pour les questionnaires PSY)
            if ("PSYCHOLOGIQUE".equalsIgnoreCase(questionnaire.getType())) {
                Map<CategorieBesoin, Integer> scoresParCat = new HashMap<>();
                for (PonderationOrientation p : ponderations) {
                    if (p.getCategorieBesoin() != null) {
                        scoresParCat.merge(p.getCategorieBesoin(), p.getPoids(), Integer::sum);
                    }
                }
                List<Map.Entry<CategorieBesoin, Integer>> classement = scoresParCat.entrySet().stream()
                        .sorted(Map.Entry.<CategorieBesoin, Integer>comparingByValue().reversed())
                        .toList();

                final ResultatOrientation savedResultat = resultat;
                for (int i = 0; i < classement.size(); i++) {
                    ResultatOrientationCategorie roc = new ResultatOrientationCategorie();
                    roc.setResultatOrientation(savedResultat);
                    roc.setCategorieBesoin(classement.get(i).getKey());
                    roc.setScore(classement.get(i).getValue());
                    roc.setRang(i + 1);
                    resultatCategorieRepository.save(roc);
                }
                resultat = resultatOrientationRepository.findById(resultat.getId()).orElse(resultat);
            }
        } else {
            // Mode anonyme (sans compte connecté) : on calcule et renseigne les scores en mémoire pour le DTO
            if ("PSYCHOLOGIQUE".equalsIgnoreCase(questionnaire.getType())) {
                Map<CategorieBesoin, Integer> scoresParCat = new HashMap<>();
                for (PonderationOrientation p : ponderations) {
                    if (p.getCategorieBesoin() != null) {
                        scoresParCat.merge(p.getCategorieBesoin(), p.getPoids(), Integer::sum);
                    }
                }
                List<Map.Entry<CategorieBesoin, Integer>> classement = scoresParCat.entrySet().stream()
                        .sorted(Map.Entry.<CategorieBesoin, Integer>comparingByValue().reversed())
                        .toList();

                List<ResultatOrientationCategorie> scoresMem = new ArrayList<>();
                for (int i = 0; i < classement.size(); i++) {
                    ResultatOrientationCategorie roc = new ResultatOrientationCategorie();
                    roc.setCategorieBesoin(classement.get(i).getKey());
                    roc.setScore(classement.get(i).getValue());
                    roc.setRang(i + 1);
                    scoresMem.add(roc);
                }
                resultat.setScoresParCategorie(scoresMem);
            }
        }

        // 14. Retourner le Response DTO
        return toResultatDto(resultat, statutDepartage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultatOrientationDTO> getMesResultats() {
        Utilisateur user = obtenirUtilisateurCourant();
        if (user == null) {
            return Collections.emptyList();
        }
        return resultatOrientationRepository.findByUtilisateurIdOrderByDateEvaluationDesc(user.getId())
                .stream()
                .map(r -> toResultatDto(r, "ARCHIVE"))
                .toList();
    }

    private Reponse trouverReponseQuestionOrdre(List<Reponse> reponses, int ordre, String codePrefix) {
        return reponses.stream()
                .filter(r -> r.getQuestion() != null &&
                        ((r.getQuestion().getOrdre() != null && r.getQuestion().getOrdre() == ordre) ||
                         (r.getQuestion().getCode() != null && r.getQuestion().getCode().startsWith(codePrefix))))
                .findFirst()
                .orElse(null);
    }

    private Utilisateur obtenirUtilisateurCourant() {
        try {
            String uid = authenticationContext.getFirebaseUid().orElse(null);
            if (uid != null) {
                return utilisateurRepository.findById(uid).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Aucun contexte d'authentification disponible : {}", e.getMessage());
        }
        return null;
    }

    private QuestionnaireDTO toQuestionnaireDto(Questionnaire q) {
        List<QuestionDTO> questionsDto = q.getQuestions() != null ? q.getQuestions().stream()
                .sorted(Comparator.comparingInt(quest -> quest.getOrdre() != null ? quest.getOrdre() : 0))
                .map(this::toQuestionDto)
                .toList() : Collections.emptyList();

        return QuestionnaireDTO.builder()
                .id(q.getId())
                .code(q.getCode())
                .titre(q.getTitre())
                .type(q.getType())
                .actif(q.getActif())
                .questions(questionsDto)
                .build();
    }

    private QuestionDTO toQuestionDto(Question quest) {
        List<ReponseDTO> reponsesDto = quest.getReponses() != null ? quest.getReponses().stream()
                .map(r -> ReponseDTO.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .libelle(r.getLibelle())
                        .valeur(r.getValeur())
                        .poids(r.getPoids())
                        .questionId(quest.getId())
                        .build())
                .toList() : Collections.emptyList();

        return QuestionDTO.builder()
                .id(quest.getId())
                .code(quest.getCode())
                .texte(quest.getTexte())
                .ordre(quest.getOrdre())
                .obligatoire(quest.getObligatoire())
                .contexte(quest.getContexte())
                .typeReponse(quest.getTypeReponse())
                .reponses(reponsesDto)
                .build();
    }

    private ResultatOrientationDTO toResultatDto(ResultatOrientation r, String statutDepartage) {
        List<ProfessionnelResponseDTO> prosDto = r.getProfessionnelsRecommandes() != null ?
                r.getProfessionnelsRecommandes().stream()
                        .map(professionnelMapper::toDto)
                        .toList() : Collections.emptyList();

        // Mapper le classement complet multi-catégories
        List<ResultatOrientationCategorieDTO> scoresDto = r.getScoresParCategorie() != null ?
                r.getScoresParCategorie().stream()
                        .sorted(Comparator.comparingInt(ResultatOrientationCategorie::getRang))
                        .map(roc -> ResultatOrientationCategorieDTO.builder()
                                .id(roc.getId())
                                .categorieBesoinId(roc.getCategorieBesoin() != null ? roc.getCategorieBesoin().getId() : null)
                                .categorieBesoinNom(roc.getCategorieBesoin() != null ? roc.getCategorieBesoin().getNom() : null)
                                .categorieBesoinDescription(roc.getCategorieBesoin() != null ? roc.getCategorieBesoin().getDescription() : null)
                                .score(roc.getScore())
                                .rang(roc.getRang())
                                .build())
                        .toList() : Collections.emptyList();

        return ResultatOrientationDTO.builder()
                .id(r.getId())
                .dateEvaluation(r.getDateEvaluation())
                .score(r.getScore())
                .questionnaireId(r.getQuestionnaire() != null ? r.getQuestionnaire().getId() : null)
                .questionnaireTitre(r.getQuestionnaire() != null ? r.getQuestionnaire().getTitre() : null)
                .categorieBesoinId(r.getCategorieBesoin() != null ? r.getCategorieBesoin().getId() : null)
                .categorieBesoinCode(r.getCategorieBesoin() != null ? r.getCategorieBesoin().getCode() : null)
                .categorieBesoinNom(r.getCategorieBesoin() != null ? r.getCategorieBesoin().getNom() : null)
                .categorieBesoinDescription(r.getCategorieBesoin() != null ? r.getCategorieBesoin().getDescription() : null)
                .statutDepartage(statutDepartage)
                .specialiteId(r.getSpecialite() != null ? r.getSpecialite().getId() : null)
                .specialiteNom(r.getSpecialite() != null ? r.getSpecialite().getNom() : null)
                .specialiteDescription(r.getSpecialite() != null ? r.getSpecialite().getDescription() : null)
                .domaineNom(r.getSpecialite() != null && r.getSpecialite().getDomaine() != null ?
                        r.getSpecialite().getDomaine().getNom() : null)
                .scoresParCategorie(scoresDto)
                .professionnelsRecommandes(prosDto)
                .build();
    }
}
