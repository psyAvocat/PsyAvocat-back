package com.psyavocat.config;

import com.psyavocat.entity.Domaine;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.DomaineRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.service.support.DomainesFixes;
import com.psyavocat.service.support.TypesProfessionnel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Initialisation des deux domaines fixes « Psychologique » et « Juridique ».
 *
 * Idempotente : elle ne crée chaque domaine qu'une seule fois (repéré par son code) ;
 * aux démarrages suivants elle ne modifie rien. Un domaine existant de même nom et de même
 * univers est repris tel quel (identifiant et description conservés).
 *
 * Elle rattache ensuite chaque spécialité au domaine de son univers et retire les anciens
 * domaines créés à la main, devenus vides. Exécutée après {@link ReferentielTypeMigration},
 * qui qualifie l'univers des spécialités anciennes.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DomainesFixesInitializer implements ApplicationRunner {

    private final DomaineRepository domaineRepository;
    private final SpecialiteRepository specialiteRepository;

    public DomainesFixesInitializer(DomaineRepository domaineRepository, SpecialiteRepository specialiteRepository) {
        this.domaineRepository = domaineRepository;
        this.specialiteRepository = specialiteRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<DomainesFixes, Domaine> fixes = new EnumMap<>(DomainesFixes.class);
        for (DomainesFixes d : DomainesFixes.values()) {
            fixes.put(d, assurerDomaine(d));
        }

        // Chaque spécialité qualifiée rejoint le domaine de son univers.
        for (Specialite s : specialiteRepository.findAll()) {
            DomainesFixes attendu = DomainesFixes.pourType(TypesProfessionnel.typeDe(s));
            Domaine cible = attendu != null ? fixes.get(attendu) : null;
            Domaine actuel = s.getDomaine();
            boolean actuelFixe = actuel != null && DomainesFixes.estCodeFixe(actuel.getCode());
            if (cible != null && !Objects.equals(actuel != null ? actuel.getId() : null, cible.getId())) {
                s.setDomaine(cible);
                specialiteRepository.save(s);
            } else if (cible == null && actuel != null && !actuelFixe) {
                // Spécialité sans univers : détachée de l'ancien domaine, à qualifier par l'administration.
                s.setDomaine(null);
                specialiteRepository.save(s);
            }
        }

        // Les anciens domaines saisis à la main ne contiennent plus de spécialité : ils sont retirés.
        List<Domaine> anciens = domaineRepository.findAll().stream()
                .filter(d -> !DomainesFixes.estCodeFixe(d.getCode()))
                .toList();
        for (Domaine ancien : anciens) {
            if (specialiteRepository.countByDomaineId(ancien.getId()) == 0) {
                domaineRepository.delete(ancien);
                log.info("[Domaines] Ancien domaine « {} » retiré : remplacé par les domaines fixes.", ancien.getNom());
            }
        }
    }

    private Domaine assurerDomaine(DomainesFixes fixe) {
        Domaine domaine = domaineRepository.findByCode(fixe.code()).orElse(null);
        if (domaine != null) {
            return domaine;
        }

        // Reprise d'un domaine existant de même nom (sans univers contradictoire).
        domaine = domaineRepository.findAll().stream()
                .filter(d -> fixe.nom().equalsIgnoreCase(d.getNom() != null ? d.getNom().trim() : null))
                .filter(d -> d.getTypeProfessionnel() == null || fixe.typeProfessionnel().equals(d.getTypeProfessionnel()))
                .findFirst()
                .orElse(null);
        if (domaine == null) {
            // Un autre domaine porte déjà ce nom (univers différent) : il est renommé pour libérer le nom.
            domaineRepository.findByNom(fixe.nom()).ifPresent(conflit -> {
                conflit.setNom(conflit.getNom() + " (ancien)");
                domaineRepository.saveAndFlush(conflit);
            });
            domaine = new Domaine();
            domaine.setDescription(fixe.descriptionParDefaut());
            log.info("[Domaines] Création du domaine fixe « {} ».", fixe.nom());
        } else {
            log.info("[Domaines] Domaine existant « {} » repris comme domaine fixe.", domaine.getNom());
        }
        domaine.setCode(fixe.code());
        domaine.setNom(fixe.nom());
        domaine.setTypeProfessionnel(fixe.typeProfessionnel());
        if (domaine.getDescription() == null || domaine.getDescription().isBlank()) {
            domaine.setDescription(fixe.descriptionParDefaut());
        }
        return domaineRepository.save(domaine);
    }
}
