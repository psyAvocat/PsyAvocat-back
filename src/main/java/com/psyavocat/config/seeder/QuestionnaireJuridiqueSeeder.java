package com.psyavocat.config.seeder;

import com.psyavocat.entity.PonderationOrientation;
import com.psyavocat.entity.Question;
import com.psyavocat.entity.Questionnaire;
import com.psyavocat.entity.Reponse;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.PonderationOrientationRepository;
import com.psyavocat.repository.QuestionRepository;
import com.psyavocat.repository.QuestionnaireRepository;
import com.psyavocat.repository.ReponseRepository;
import com.psyavocat.repository.SpecialiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeder dédié au Questionnaire Juridique V1.
 * 100% Idempotent : Vérifie l'existence par type "JURIDIQUE".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionnaireJuridiqueSeeder {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final ReponseRepository reponseRepository;
    private final PonderationOrientationRepository ponderationOrientationRepository;
    private final SpecialiteRepository specialiteRepository;

    @Transactional
    public void seed() {
        if (questionnaireRepository.existsByType("JURIDIQUE")) {
            return;
        }

        log.info("  [QuestionnaireJuridiqueSeeder] Initialisation du Questionnaire Juridique...");

        Questionnaire q = new Questionnaire();
        q.setCode("QUESTIONNAIRE_ORIENTATION_JURIDIQUE_V1");
        q.setTitre("Questionnaire d'orientation juridique");
        q.setType("JURIDIQUE");
        q.setActif(true);
        q = questionnaireRepository.save(q);

        Specialite specTravail = findSpecialite("Droit du travail & relations sociales");
        Specialite specFamille = findSpecialite("Droit de la famille & des personnes");
        Specialite specFoncier = findSpecialite("Droit immobilier & contentieux foncier");
        Specialite specPenal = findSpecialite("Droit pénal & défense criminelle");
        Specialite specAffaires = findSpecialite("Droit des affaires & contentieux commercial");

        // Q1 : Domaine principal
        Question q1 = saveQuestion(q, "Q01_JUR", "Quel est le domaine principal de votre préoccupation juridique ?", 1);
        Reponse r1_1 = saveReponse(q1, "Q01_JUR_R01", "Un litige avec mon employeur ou sur mon contrat de travail", "TRAVAIL");
        savePonderation(r1_1, specTravail, 20);

        Reponse r1_2 = saveReponse(q1, "Q01_JUR_R02", "Une affaire de famille : séparation, divorce, garde d'enfants ou succession", "FAMILLE");
        savePonderation(r1_2, specFamille, 20);

        Reponse r1_3 = saveReponse(q1, "Q01_JUR_R03", "Un problème de terrain, litige foncier, loyers impayés ou titre de propriété", "FONCIER");
        savePonderation(r1_3, specFoncier, 20);

        Reponse r1_4 = saveReponse(q1, "Q01_JUR_R04", "Une infraction, convocation de police, garde à vue ou plainte pénale", "PENAL");
        savePonderation(r1_4, specPenal, 20);

        Reponse r1_5 = saveReponse(q1, "Q01_JUR_R05", "Un litige commercial, impayé d'entreprise ou conflit entre associés", "AFFAIRES");
        savePonderation(r1_5, specAffaires, 20);

        // Q2 : Urgence de la situation
        Question q2 = saveQuestion(q, "Q02_JUR", "Quel est le niveau d'urgence de votre démarche ?", 2);
        Reponse r2_1 = saveReponse(q2, "Q02_JUR_R01", "Très urgent : une convocation au tribunal ou délai de recours sous 7 jours", "TRES_URGENT");
        savePonderation(r2_1, specPenal, 10);

        Reponse r2_2 = saveReponse(q2, "Q02_JUR_R02", "Important : mise en demeure reçue ou conflit ouvert nécessitant un conseil rapide", "URGENT");
        savePonderation(r2_2, specTravail, 5);

        Reponse r2_3 = saveReponse(q2, "Q02_JUR_R03", "Préventif : je souhaite anticiper, sécuriser mes droits ou faire relire un accord", "PREVENTIF");
        savePonderation(r2_3, specAffaires, 5);

        // Q3 : Document officiel existant
        Question q3 = saveQuestion(q, "Q03_JUR", "Disposez-vous déjà d'un acte officiel (assignation, sommation, lettre de licenciement) ?", 3);
        Reponse r3_1 = saveReponse(q3, "Q03_JUR_R01", "Oui, j'ai reçu un acte officiel avec une date limite précise", "ACTE_OFFICIEL");
        savePonderation(r3_1, specTravail, 10);
        savePonderation(r3_1, specFoncier, 10);

        Reponse r3_2 = saveReponse(q3, "Q03_JUR_R02", "Des courriers recommandés ou échanges d'emails formels", "COURRIERS");
        savePonderation(r3_2, specFamille, 5);

        Reponse r3_3 = saveReponse(q3, "Q03_JUR_R03", "Non, les désaccords sont encore informels pour le moment", "INFORMEL");
        savePonderation(r3_3, specFamille, 5);

        // Q4 : Objectif recherché
        Question q4 = saveQuestion(q, "Q04_JUR", "Quelle issue privilégiez-vous en priorité ?", 4);
        Reponse r4_1 = saveReponse(q4, "Q04_JUR_R01", "Une négociation amiable et un accord transactionnel confidentiel", "AMIABLE");
        savePonderation(r4_1, specTravail, 5);
        savePonderation(r4_1, specFamille, 5);

        Reponse r4_2 = saveReponse(q4, "Q04_JUR_R02", "Une action ferme en justice devant les tribunaux pour faire valoir mes droits", "CONTENTIEUX");
        savePonderation(r4_2, specPenal, 10);
        savePonderation(r4_2, specFoncier, 10);

        Reponse r4_3 = saveReponse(q4, "Q04_JUR_R03", "Un avis éclairé pour connaître mes chances de succès et évaluer les risques", "CONSEIL");
        savePonderation(r4_3, specAffaires, 5);

        // Q5 : Mode de consultation souhaité
        Question q5 = saveQuestion(q, "Q05_JUR", "Comment préférez-vous échanger avec votre avocat ?", 5);
        saveReponse(q5, "Q05_JUR_R01", "En visioconférence sécurisée depuis chez moi", "VISIO");
        saveReponse(q5, "Q05_JUR_R02", "En cabinet physique directement", "CABINET");
        saveReponse(q5, "Q05_JUR_R03", "Le plus rapide selon les disponibilités du professionnel", "RAPIDE");

        log.info("    + Questionnaire Juridique complet inséré avec pondérations !");
    }

    private Specialite findSpecialite(String nom) {
        return specialiteRepository.findByNomIgnoreCase(nom).orElse(null);
    }

    private Question saveQuestion(Questionnaire q, String code, String texte, int ordre) {
        Question qu = new Question();
        qu.setCode(code);
        qu.setTexte(texte);
        qu.setOrdre(ordre);
        qu.setObligatoire(true);
        qu.setQuestionnaire(q);
        return questionRepository.save(qu);
    }

    private Reponse saveReponse(Question qu, String code, String libelle, String valeur) {
        Reponse r = new Reponse();
        r.setCode(code);
        r.setLibelle(libelle);
        r.setValeur(valeur);
        r.setPoids(0);
        r.setQuestion(qu);
        return reponseRepository.save(r);
    }

    private void savePonderation(Reponse rep, Specialite spec, int poids) {
        if (spec == null) return;
        PonderationOrientation p = new PonderationOrientation();
        p.setReponse(rep);
        p.setCategorieBesoin(null);
        p.setSpecialite(spec);
        p.setPoids(poids);
        ponderationOrientationRepository.save(p);
    }
}
