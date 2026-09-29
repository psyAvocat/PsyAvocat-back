package com.psyavocat.service.impl;

import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.Specialite;
import com.psyavocat.repository.ProfessionnelRepository;
import com.psyavocat.service.MatchingProfessionnelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation du service de matching des professionnels basée sur MySQL.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MatchingProfessionnelServiceImpl implements MatchingProfessionnelService {

    private final ProfessionnelRepository professionnelRepository;

    @Override
    public List<Professionnel> matcherParSpecialite(Specialite specialite, int limite) {
        if (specialite == null || specialite.getId() == null) {
            return getFallbackProfessionnels(limite);
        }

        List<Professionnel> correspondants = professionnelRepository.searchProfessionnels(
                "VALIDE",
                null,
                null,
                specialite.getId()
        );

        if (correspondants.isEmpty()) {
            correspondants = getFallbackProfessionnels(limite);
        }

        return correspondants.stream().limit(limite).toList();
    }

    @Override
    public List<Professionnel> matcherParCategorieBesoin(CategorieBesoin categorieBesoin, int limite) {
        if (categorieBesoin == null) {
            return getFallbackProfessionnels(limite);
        }

        List<Professionnel> correspondants = new ArrayList<>();

        // Si la catégorie de besoin est liée à des spécialités
        if (categorieBesoin.getSpecialites() != null && !categorieBesoin.getSpecialites().isEmpty()) {
            for (Specialite spec : categorieBesoin.getSpecialites()) {
                List<Professionnel> list = professionnelRepository.searchProfessionnels(
                        "VALIDE",
                        null,
                        null,
                        spec.getId()
                );
                for (Professionnel p : list) {
                    if (!correspondants.contains(p)) {
                        correspondants.add(p);
                    }
                }
            }
        }

        // Si aucun pro par spécialité directe, chercher tous les psychologues validés
        if (correspondants.isEmpty()) {
            List<Professionnel> allValides = professionnelRepository.findByStatutValidation("VALIDE");
            correspondants = allValides.stream()
                    .filter(p -> p instanceof Psychologue)
                    .toList();
        }

        if (correspondants.isEmpty()) {
            correspondants = getFallbackProfessionnels(limite);
        }

        return correspondants.stream().limit(limite).toList();
    }

    private List<Professionnel> getFallbackProfessionnels(int limite) {
        return professionnelRepository.findByStatutValidation("VALIDE")
                .stream()
                .limit(limite)
                .toList();
    }
}
