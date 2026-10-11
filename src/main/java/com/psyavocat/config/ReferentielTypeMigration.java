package com.psyavocat.config;

import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.Domaine;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.DomaineRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.service.support.TypesProfessionnel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Migration idempotente : renseigne le type de professionnel (AVOCAT / PSYCHOLOGUE) des
 * référentiels créés avant qu'il ne soit obligatoire.
 *
 * Le type est déduit UNIQUEMENT des relations réelles et sans ambiguïté :
 * - spécialité : types de ses catégories de besoin, de son domaine et des professionnels qui l'utilisent ;
 * - domaine : types de ses spécialités ;
 * - catégorie : types des spécialités liées.
 * Aucun libellé n'est interprété. En cas d'ambiguïté ou d'absence de relation, l'élément reste
 * non qualifié (exclu des listes par univers) et est signalé pour correction par l'administration.
 */
@Slf4j
@Component
public class ReferentielTypeMigration implements ApplicationRunner {

    private final SpecialiteRepository specialiteRepository;
    private final DomaineRepository domaineRepository;
    private final CategorieBesoinRepository categorieBesoinRepository;

    public ReferentielTypeMigration(SpecialiteRepository specialiteRepository,
                                    DomaineRepository domaineRepository,
                                    CategorieBesoinRepository categorieBesoinRepository) {
        this.specialiteRepository = specialiteRepository;
        this.domaineRepository = domaineRepository;
        this.categorieBesoinRepository = categorieBesoinRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Specialite s : specialiteRepository.findAll()) {
            if (TypesProfessionnel.typeDe(s) != null) {
                continue;
            }
            Set<String> types = Stream.of(
                            s.getCategoriesBesoin().stream().map(c -> valide(c.getTypeProfessionnel())),
                            Stream.of(s.getDomaine() != null ? valide(s.getDomaine().getTypeProfessionnel()) : null),
                            s.getProfessionnels().stream().map(TypesProfessionnel::typeDe))
                    .flatMap(x -> x)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (types.size() == 1) {
                s.setTypeProfessionnel(types.iterator().next());
                specialiteRepository.save(s);
                log.info("[Référentiels] Spécialité « {} » qualifiée {} d'après ses relations.", s.getNom(), s.getTypeProfessionnel());
            } else {
                log.warn("[Référentiels] Spécialité « {} » sans univers déterminable ({}) : à qualifier par l'administration.",
                        s.getNom(), types.isEmpty() ? "aucune relation" : "relations contradictoires " + types);
            }
        }

        for (Domaine d : domaineRepository.findAll()) {
            if (valide(d.getTypeProfessionnel()) != null) {
                continue;
            }
            Set<String> types = d.getSpecialites().stream()
                    .map(TypesProfessionnel::typeDe)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (types.size() == 1) {
                d.setTypeProfessionnel(types.iterator().next());
                domaineRepository.save(d);
                log.info("[Référentiels] Domaine « {} » qualifié {} d'après ses spécialités.", d.getNom(), d.getTypeProfessionnel());
            } else {
                log.warn("[Référentiels] Domaine « {} » sans univers déterminable : à qualifier par l'administration.", d.getNom());
            }
        }

        for (CategorieBesoin c : categorieBesoinRepository.findAll()) {
            if (valide(c.getTypeProfessionnel()) != null) {
                continue;
            }
            Set<String> types = c.getSpecialites().stream()
                    .map(TypesProfessionnel::typeDe)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (types.size() == 1) {
                c.setTypeProfessionnel(types.iterator().next());
                categorieBesoinRepository.save(c);
                log.info("[Référentiels] Catégorie « {} » qualifiée {} d'après ses spécialités.", c.getNom(), c.getTypeProfessionnel());
            } else {
                log.warn("[Référentiels] Catégorie de besoin « {} » sans univers déterminable : à qualifier par l'administration.", c.getNom());
            }
        }
    }

    /** Valeur reconnue (AVOCAT / PSYCHOLOGUE) ou null, sans lever d'erreur au démarrage. */
    private String valide(String type) {
        if (type == null) {
            return null;
        }
        String t = type.trim().toUpperCase();
        return TypesProfessionnel.AVOCAT.equals(t) || TypesProfessionnel.PSYCHOLOGUE.equals(t) ? t : null;
    }
}
