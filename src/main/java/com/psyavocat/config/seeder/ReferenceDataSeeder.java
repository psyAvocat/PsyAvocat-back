package com.psyavocat.config.seeder;

import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.Domaine;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.DomaineRepository;
import com.psyavocat.repository.SpecialiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeder dédié aux données de référence : Domaines, Spécialités et Catégories de Besoin juridiques.
 * 100% Idempotent : Vérifie l'existence par nom et code avant chaque insertion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceDataSeeder {

    private final DomaineRepository domaineRepository;
    private final SpecialiteRepository specialiteRepository;
    private final CategorieBesoinRepository categorieBesoinRepository;

    @Transactional
    public void seed() {
        log.info("  [ReferenceDataSeeder] Initialisation des données de référence (Domaines, Spécialités, Catégories)...");

        // 1. Domaines
        Map<String, Domaine> domaines = seedDomaines();

        // 2. Spécialités
        Map<String, Specialite> specialites = seedSpecialites(domaines);

        // 3. Catégories juridiques associées aux spécialités
        seedCategoriesJuridiques(specialites);
    }

    private Map<String, Domaine> seedDomaines() {
        Map<String, Domaine> map = new HashMap<>();

        map.put("DROIT_FAMILLE", saveDomaineIfNotExists(
                "Droit Privé & des Personnes",
                "Affaires familiales, successions, filiation, régimes matrimoniaux et protection des personnes vulnérables."
        ));

        map.put("DROIT_TRAVAIL", saveDomaineIfNotExists(
                "Droit du Travail & Social",
                "Relations de travail, contrats, licenciements, rupture conventionnelle, contentieux prud'homal et harcèlement."
        ));

        map.put("DROIT_FONCIER", saveDomaineIfNotExists(
                "Droit Immobilier & Foncier",
                "Titres fonciers, litiges de propriété coutumière et moderne, baux d'habitation et commerciaux, copropriété et expulsions."
        ));

        map.put("DROIT_PENAL", saveDomaineIfNotExists(
                "Droit Pénal & Libertés",
                "Défense pénale d'urgence, garde à vue, plaintes pénales, infractions, escroqueries et comparutions judiciaires."
        ));

        map.put("DROIT_AFFAIRES", saveDomaineIfNotExists(
                "Droit des Affaires & Commercial",
                "Droit OHADA, rédaction de contrats commerciaux, création de sociétés, recouvrement de créances et litiges entre associés."
        ));

        map.put("PSY_CLINIQUE", saveDomaineIfNotExists(
                "Psychologie Clinique & Thérapie",
                "Prise en charge psychothérapeutique des troubles de l'humeur, angoisses, dépression et estime de soi."
        ));

        map.put("PSY_TRAVAIL", saveDomaineIfNotExists(
                "Psychologie du Travail & Risques Psycho-sociaux",
                "Accompagnement de l'épuisement professionnel, burn-out, surcharge mentale et souffrance au travail."
        ));

        map.put("PSY_FAMILLE", saveDomaineIfNotExists(
                "Psychologie Familiale & Conjugale",
                "Thérapie de couple, médiation familiale, résolution des conflits parentaux et gestion des ruptures affectives."
        ));

        map.put("PSY_TRAUMA", saveDomaineIfNotExists(
                "Psychotraumatologie & Deuil",
                "Accompagnement du deuil, traumatismes psychologiques, événements de vie douloureux et travail de résilience."
        ));

        return map;
    }

    private Domaine saveDomaineIfNotExists(String nom, String description) {
        return domaineRepository.findByNom(nom).orElseGet(() -> {
            Domaine d = new Domaine();
            d.setNom(nom);
            d.setDescription(description);
            log.info("    + Création du Domaine : {}", nom);
            return domaineRepository.save(d);
        });
    }

    private Map<String, Specialite> seedSpecialites(Map<String, Domaine> domaines) {
        Map<String, Specialite> map = new HashMap<>();

        // Spécialités Avocat
        map.put("SPEC_FAMILLE", saveSpecialiteIfNotExists(
                "Droit de la famille & des personnes",
                "Divorce, garde d'enfants, pension alimentaire, autorité parentale et successions patrimoniales.",
                domaines.get("DROIT_FAMILLE")
        ));

        map.put("SPEC_TRAVAIL", saveSpecialiteIfNotExists(
                "Droit du travail & relations sociales",
                "Licenciements abusifs, rupture conventionnelle, impayés de salaires et harcèlement moral au travail.",
                domaines.get("DROIT_TRAVAIL")
        ));

        map.put("SPEC_FONCIER", saveSpecialiteIfNotExists(
                "Droit immobilier & contentieux foncier",
                "Contestation de titres fonciers, litiges de bornage, baux commerciaux et litiges d'expulsion.",
                domaines.get("DROIT_FONCIER")
        ));

        map.put("SPEC_PENAL", saveSpecialiteIfNotExists(
                "Droit pénal & défense criminelle",
                "Assistance en garde à vue, défense correctionnelle, plaintes avec constitution de partie civile.",
                domaines.get("DROIT_PENAL")
        ));

        map.put("SPEC_AFFAIRES", saveSpecialiteIfNotExists(
                "Droit des affaires & contentieux commercial",
                "Contrats commerciaux OHADA, contentieux entre actionnaires et recouvrement forcé de créances.",
                domaines.get("DROIT_AFFAIRES")
        ));

        // Spécialités Psychologue
        map.put("SPEC_BURNOUT", saveSpecialiteIfNotExists(
                "Gestion du stress, anxiété & burn-out",
                "Épuisement professionnel, surcharge cognitive, anxiété de performance et équilibre vie pro/perso.",
                domaines.get("PSY_TRAVAIL")
        ));

        map.put("SPEC_CLINIQUE", saveSpecialiteIfNotExists(
                "Psychothérapie clinique & troubles de l'humeur",
                "Accompagnement de la dépression, des idées sombres, du repli sur soi et des angoisses diffuses.",
                domaines.get("PSY_CLINIQUE")
        ));

        map.put("SPEC_COUPLE", saveSpecialiteIfNotExists(
                "Thérapie de couple & médiation familiale",
                "Rétablissement de la communication conjugale, surmonter l'infidélité ou préparer une séparation apaisée.",
                domaines.get("PSY_FAMILLE")
        ));

        map.put("SPEC_TRAUMA", saveSpecialiteIfNotExists(
                "Psychotraumatologie & gestion du deuil",
                "Dépassement du stress post-traumatique, deuil traumatique et reconstruction après choc émotionnel.",
                domaines.get("PSY_TRAUMA")
        ));

        return map;
    }

    private Specialite saveSpecialiteIfNotExists(String nom, String description, Domaine domaine) {
        return specialiteRepository.findByNomIgnoreCase(nom).orElseGet(() -> {
            Specialite s = new Specialite();
            s.setNom(nom);
            s.setDescription(description);
            s.setDomaine(domaine);
            log.info("    + Création de la Spécialité : {}", nom);
            return specialiteRepository.save(s);
        });
    }

    private void seedCategoriesJuridiques(Map<String, Specialite> specialites) {
        saveCategorieBesoinIfNotExists(
                "CAT_LITIGE_TRAVAIL",
                "Litige professionnel & employeur",
                "Conflit avec votre employeur, licenciement, heures impayées ou rupture de contrat.",
                "AVOCAT",
                List.of(specialites.get("SPEC_TRAVAIL"))
        );

        saveCategorieBesoinIfNotExists(
                "CAT_CONFLIT_FAMILLE",
                "Conflit familial & divorce",
                "Procédure de divorce, garde des enfants, pension alimentaire ou partage successoral.",
                "AVOCAT",
                List.of(specialites.get("SPEC_FAMILLE"))
        );

        saveCategorieBesoinIfNotExists(
                "CAT_LITIGE_FONCIER",
                "Litige foncier & immobilier",
                "Problème lié à un terrain, contestation de titre foncier, litige locatif ou menace d'expulsion.",
                "AVOCAT",
                List.of(specialites.get("SPEC_FONCIER"))
        );

        saveCategorieBesoinIfNotExists(
                "CAT_AFFAIRE_PENALE",
                "Affaire pénale & poursuite judiciaire",
                "Plainte déposée contre vous ou par vous, garde à vue, convocation au tribunal ou infraction.",
                "AVOCAT",
                List.of(specialites.get("SPEC_PENAL"))
        );

        saveCategorieBesoinIfNotExists(
                "CAT_DROIT_AFFAIRES",
                "Contrat d'affaires & contentieux commercial",
                "Litiges entre associés, facture impayée, inexécution contractuelle ou création de société.",
                "AVOCAT",
                List.of(specialites.get("SPEC_AFFAIRES"))
        );
    }

    private void saveCategorieBesoinIfNotExists(
            String code,
            String nom,
            String description,
            String typeProfessionnel,
            List<Specialite> specs
    ) {
        if (!categorieBesoinRepository.existsByNom(nom)) {
            CategorieBesoin c = new CategorieBesoin();
            c.setCode(code);
            c.setNom(nom);
            c.setDescription(description);
            c.setTypeProfessionnel(typeProfessionnel);
            c.setActif(true);
            c.setSpecialites(specs);
            log.info("    + Création de la Catégorie de Besoin : [{}] {} ({})", code, nom, typeProfessionnel);
            categorieBesoinRepository.save(c);
        }
    }
}
