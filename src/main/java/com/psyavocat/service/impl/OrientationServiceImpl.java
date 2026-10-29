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
import com.psyavocat.service.support.TypesProfessionnel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Orientation psychologique (facultative, non diagnostique).
 *
 * Principes :
 * 1. 100 % piloté par la configuration de l'administration : questionnaire actif, questions et
 *    réponses actives, pondérations vers des catégories de besoin ACTIVES de l'univers Psychologue.
 * 2. Une configuration désactivée ou obsolète n'est jamais proposée ni acceptée.
 * 3. Les pondérations internes ne sont jamais exposées aux clients.
 * 4. Départage déterministe sans règle fondée sur un libellé ou un code de question :
 *    la première question (dans l'ordre configuré) dont la réponse pondère une catégorie à égalité
 *    l'emporte ; à défaut, ordre alphabétique des catégories.
 * 5. Recommandations : psychologues APPROVED liés aux spécialités de la catégorie ; aucun repli.
 */
@Slf4j
@Service
@Transactional
public class OrientationServiceImpl implements OrientationService {

    static final String TYPE_PSYCHOLOGIQUE = "PSYCHOLOGIQUE";
    static final String ETAT_PROFESSIONNELS_TROUVES = "PROFESSIONNELS_TROUVES";
    static final String ETAT_AUCUN_PROFESSIONNEL = "AUCUN_PROFESSIONNEL_DISPONIBLE";
    private static final int NB_RECOMMANDATIONS = 5;

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

    // ------------------------------------------------------------------
    // Lecture publique (clients)
    // ------------------------------------------------------------------

    /** Questionnaire publié : le questionnaire d'orientation psychologique (une liste d'au plus un élément). */
    @Override
    @Transactional(readOnly = true)
    public List<QuestionnaireDTO> getQuestionnaires(String type) {
        if (type != null && !type.isBlank() && !TYPE_PSYCHOLOGIQUE.equalsIgnoreCase(type.trim())) {
            return List.of();
        }
        return questionnairePublie().map(this::toQuestionnaireDto).stream().toList();
    }

    /**
     * Le questionnaire d'orientation psychologique s'il est publié (repéré par son code) ;
     * à défaut, le questionnaire psychologique actif (données antérieures à l'initialisation).
     */
    private java.util.Optional<Questionnaire> questionnairePublie() {
        return questionnaireRepository.findByCode(com.psyavocat.service.support.QuestionnaireOrientation.CODE)
                .filter(q -> Boolean.TRUE.equals(q.getActif()))
                .or(() -> questionnaireRepository.findAllByTypeAndActifTrue(TYPE_PSYCHOLOGIQUE).stream().findFirst());
    }

    /** Questionnaire publié du type demandé ; jamais de repli sur un questionnaire inactif. */
    @Override
    @Transactional(readOnly = true)
    public QuestionnaireDTO getQuestionnaireByType(String type) {
        if (type == null || !TYPE_PSYCHOLOGIQUE.equalsIgnoreCase(type.trim())) {
            throw new ResourceNotFoundException("Aucun questionnaire d'orientation n'est proposé pour ce parcours.");
        }
        return questionnairePublie()
                .map(this::toQuestionnaireDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun questionnaire d'orientation n'est publié pour le moment."));
    }

    // ------------------------------------------------------------------
    // Évaluation
    // ------------------------------------------------------------------

    @Override
    public ResultatOrientationDTO evaluerQuestionnaire(SoumissionQuestionnaireRequest request) {
        Questionnaire questionnaire = questionnaireRepository.findById(request.getQuestionnaireId())
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire introuvable : " + request.getQuestionnaireId()));

        if (!Boolean.TRUE.equals(questionnaire.getActif()) || !TYPE_PSYCHOLOGIQUE.equalsIgnoreCase(questionnaire.getType())) {
            throw new BadRequestException("Ce questionnaire n'est plus proposé. Rechargez le questionnaire actuel.");
        }
        if (request.getReponseIds() == null || request.getReponseIds().isEmpty()) {
            throw new BadRequestException("La liste des réponses soumises ne peut pas être vide");
        }

        List<Reponse> reponses = reponseRepository.findAllById(request.getReponseIds());
        if (reponses.size() != request.getReponseIds().stream().distinct().count()) {
            throw new BadRequestException("Certaines réponses ne sont plus disponibles. Rechargez le questionnaire actuel.");
        }

        // Chaque réponse doit appartenir au questionnaire et être active, ainsi que sa question.
        for (Reponse r : reponses) {
            Question q = r.getQuestion();
            if (q == null || q.getQuestionnaire() == null || !q.getQuestionnaire().getId().equals(questionnaire.getId())) {
                throw new BadRequestException("La réponse [" + r.getId() + "] n'appartient pas au questionnaire sélectionné");
            }
            if (Boolean.FALSE.equals(r.getActif()) || Boolean.FALSE.equals(q.getActif())) {
                throw new BadRequestException("Le questionnaire a été modifié depuis son ouverture. Rechargez-le pour continuer.");
            }
        }

        Map<String, List<Reponse>> reponsesParQuestion = reponses.stream()
                .collect(Collectors.groupingBy(r -> r.getQuestion().getId()));
        for (List<Reponse> groupe : reponsesParQuestion.values()) {
            Question q = groupe.get(0).getQuestion();
            if (groupe.size() > 1 && !"CHOIX_MULTIPLE".equalsIgnoreCase(q.getTypeReponse())) {
                throw new BadRequestException("Une seule réponse est autorisée pour la question : " + q.getTexte());
            }
        }

        List<Question> questionsActives = questionRepository.findByQuestionnaireIdOrderByOrdreAsc(questionnaire.getId()).stream()
                .filter(q -> !Boolean.FALSE.equals(q.getActif()))
                .toList();
        for (Question q : questionsActives) {
            if (Boolean.TRUE.equals(q.getObligatoire()) && !reponsesParQuestion.containsKey(q.getId())) {
                throw new BadRequestException("La question obligatoire « " + q.getTexte() + " » n'a pas reçu de réponse");
            }
        }

        // Pondérations utiles : catégories de besoin ACTIVES de l'univers Psychologue uniquement.
        List<PonderationOrientation> ponderations = ponderationOrientationRepository
                .findByReponseIdIn(reponses.stream().map(Reponse::getId).toList()).stream()
                .filter(p -> categorieUtilisable(p.getCategorieBesoin()))
                .toList();

        Map<CategorieBesoin, Integer> scores = new LinkedHashMap<>();
        for (PonderationOrientation p : ponderations) {
            scores.merge(p.getCategorieBesoin(), p.getPoids() != null ? p.getPoids() : 0, Integer::sum);
        }
        if (scores.isEmpty()) {
            throw new BadRequestException("Vos réponses ne permettent pas encore de proposer une orientation. "
                    + "Vous pouvez consulter directement la liste des psychologues.");
        }

        int maxScore = Collections.max(scores.values());
        List<CategorieBesoin> exAequo = scores.entrySet().stream()
                .filter(e -> e.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .toList();

        CategorieBesoin categorieGagnante;
        String statutDepartage;
        if (exAequo.size() == 1) {
            categorieGagnante = exAequo.get(0);
            statutDepartage = "SCORE_UNIQUE_MAXIMAL";
        } else {
            categorieGagnante = departager(exAequo, reponses, ponderations);
            statutDepartage = categorieGagnante != null ? "EGALITE_DEPARTAGEE_PAR_ORDRE_DES_QUESTIONS" : null;
            if (categorieGagnante == null) {
                categorieGagnante = exAequo.stream()
                        .min(Comparator.comparing(c -> Objects.toString(c.getCode() != null ? c.getCode() : c.getNom(), "")))
                        .orElseThrow();
                statutDepartage = "EGALITE_DEPARTAGEE_DETERMINISTE";
            }
        }

        List<Professionnel> prosRecommandes = matchingProfessionnelService.matcherParCategorieBesoin(categorieGagnante, NB_RECOMMANDATIONS);

        List<Map.Entry<CategorieBesoin, Integer>> classement = scores.entrySet().stream()
                .sorted(Map.Entry.<CategorieBesoin, Integer>comparingByValue().reversed())
                .toList();

        ResultatOrientation resultat = new ResultatOrientation();
        resultat.setDateEvaluation(LocalDateTime.now());
        resultat.setScore((double) maxScore);
        resultat.setQuestionnaire(questionnaire);
        resultat.setCategorieBesoin(categorieGagnante);
        resultat.setProfessionnelsRecommandes(new ArrayList<>(prosRecommandes));

        resultat.setUtilisateur(obtenirUtilisateurCourant());
        resultat = resultatOrientationRepository.save(resultat);
        List<ResultatOrientationCategorie> lignes = new ArrayList<>();
        for (int i = 0; i < classement.size(); i++) {
            ResultatOrientationCategorie roc = new ResultatOrientationCategorie();
            roc.setResultatOrientation(resultat);
            roc.setCategorieBesoin(classement.get(i).getKey());
            roc.setScore(classement.get(i).getValue());
            roc.setRang(i + 1);
            resultatCategorieRepository.save(roc);
            lignes.add(roc);
        }
        resultat.setScoresParCategorie(lignes);

        return toResultatDto(resultat, statutDepartage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultatOrientationDTO> getMesResultats() {
        Utilisateur user = obtenirUtilisateurCourant();
        return resultatOrientationRepository.findByUtilisateurIdOrderByDateEvaluationDesc(user.getId())
                .stream()
                .map(r -> toResultatDto(r, "ARCHIVE"))
                .toList();
    }

    // ------------------------------------------------------------------
    // Règles internes
    // ------------------------------------------------------------------

    private boolean categorieUtilisable(CategorieBesoin c) {
        return c != null
                && !Boolean.FALSE.equals(c.getActif())
                && TypesProfessionnel.PSYCHOLOGUE.equals(TypesProfessionnel.normaliserSansErreur(c.getTypeProfessionnel()));
    }

    /**
     * Parcourt les questions répondues dans l'ordre configuré ; la première dont une réponse
     * pondère l'une des catégories ex aequo désigne la catégorie retenue (poids le plus fort
     * de cette question, puis ordre alphabétique). Renvoie null si aucune question ne départage.
     */
    private CategorieBesoin departager(List<CategorieBesoin> exAequo, List<Reponse> reponses,
                                       List<PonderationOrientation> ponderations) {
        List<Question> questionsOrdonnees = reponses.stream()
                .map(Reponse::getQuestion)
                .distinct()
                .sorted(Comparator.comparingInt(q -> q.getOrdre() != null ? q.getOrdre() : Integer.MAX_VALUE))
                .toList();
        for (Question q : questionsOrdonnees) {
            CategorieBesoin choisie = ponderations.stream()
                    .filter(p -> p.getReponse() != null && p.getReponse().getQuestion() != null
                            && q.getId().equals(p.getReponse().getQuestion().getId()))
                    .filter(p -> exAequo.contains(p.getCategorieBesoin()))
                    .sorted(Comparator.comparingInt((PonderationOrientation p) -> -(p.getPoids() != null ? p.getPoids() : 0))
                            .thenComparing(p -> Objects.toString(p.getCategorieBesoin().getNom(), "")))
                    .map(PonderationOrientation::getCategorieBesoin)
                    .findFirst()
                    .orElse(null);
            if (choisie != null) {
                return choisie;
            }
        }
        return null;
    }

    /** Utilisateur connecté (l'orientation n'est proposée qu'aux comptes connectés). */
    private Utilisateur obtenirUtilisateurCourant() {
        return utilisateurRepository.findById(authenticationContext.getRequiredFirebaseUid())
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));
    }

    // ------------------------------------------------------------------
    // Mapping (aucune pondération exposée)
    // ------------------------------------------------------------------

    private QuestionnaireDTO toQuestionnaireDto(Questionnaire q) {
        List<QuestionDTO> questionsDto = q.getQuestions() != null ? q.getQuestions().stream()
                .filter(quest -> !Boolean.FALSE.equals(quest.getActif()))
                .sorted(Comparator.comparingInt(quest -> quest.getOrdre() != null ? quest.getOrdre() : 0))
                .map(this::toQuestionDto)
                .filter(dto -> !dto.getReponses().isEmpty())
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
                .filter(r -> !Boolean.FALSE.equals(r.getActif()))
                .map(r -> ReponseDTO.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .libelle(r.getLibelle())
                        .valeur(r.getValeur())
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
                .etatRecommandation(prosDto.isEmpty() ? ETAT_AUCUN_PROFESSIONNEL : ETAT_PROFESSIONNELS_TROUVES)
                .build();
    }
}
