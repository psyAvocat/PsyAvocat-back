package com.psyavocat.service.impl;

import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.ProfessionnelRepository;
import com.psyavocat.service.MatchingProfessionnelService;
import com.psyavocat.service.support.TypesProfessionnel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matching des professionnels à partir des relations réelles du référentiel :
 * catégorie de besoin → spécialités liées (même univers) → professionnels APPROVED et actifs
 * du métier correspondant.
 *
 * Aucun repli : sans correspondance, la liste est vide (l'appelant l'indique explicitement).
 * Un avocat n'est jamais recommandé pour un besoin psychologique, et inversement.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MatchingProfessionnelServiceImpl implements MatchingProfessionnelService {

    private final ProfessionnelRepository professionnelRepository;

    @Override
    public List<Professionnel> matcherParSpecialite(Specialite specialite, int limite) {
        String type = TypesProfessionnel.typeDe(specialite);
        if (specialite == null || specialite.getId() == null || type == null) {
            return List.of();
        }
        return classer(professionnelRepository.findApprouvesParSpecialites(Set.of(specialite.getId())),
                type, Set.of(specialite.getId()), limite);
    }

    @Override
    public List<Professionnel> matcherParCategorieBesoin(CategorieBesoin categorieBesoin, int limite) {
        if (categorieBesoin == null || Boolean.FALSE.equals(categorieBesoin.getActif())) {
            return List.of();
        }
        String type = TypesProfessionnel.normaliserSansErreur(categorieBesoin.getTypeProfessionnel());
        if (type == null) {
            log.warn("Catégorie de besoin {} sans univers : aucune recommandation possible", categorieBesoin.getId());
            return List.of();
        }
        // Seules les spécialités du même univers que la catégorie comptent.
        Set<String> specialiteIds = categorieBesoin.getSpecialites().stream()
                .filter(s -> type.equals(TypesProfessionnel.typeDe(s)))
                .map(Specialite::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (specialiteIds.isEmpty()) {
            return List.of();
        }
        return classer(professionnelRepository.findApprouvesParSpecialites(specialiteIds), type, specialiteIds, limite);
    }

    /**
     * Filtre sur le métier attendu, puis classe : nombre de spécialités correspondantes (décroissant),
     * puis nom et prénom (ordre stable et déterministe).
     */
    private List<Professionnel> classer(List<Professionnel> candidats, String type, Set<String> specialiteIds, int limite) {
        return candidats.stream()
                .filter(p -> type.equals(TypesProfessionnel.typeDe(p)))
                .sorted(Comparator
                        .comparingLong((Professionnel p) -> -p.getSpecialites().stream()
                                .filter(s -> specialiteIds.contains(s.getId())).count())
                        .thenComparing(p -> Objects.toString(p.getNom(), ""), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(p -> Objects.toString(p.getPrenom(), ""), String.CASE_INSENSITIVE_ORDER))
                .limit(limite)
                .toList();
    }
}
