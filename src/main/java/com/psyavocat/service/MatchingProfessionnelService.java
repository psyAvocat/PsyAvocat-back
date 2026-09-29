package com.psyavocat.service;

import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Specialite;

import java.util.List;

/**
 * Service dédié au matching des professionnels
 * en fonction des besoins d'orientation (Spécialité juridique ou Catégorie de Besoin psychologique).
 */
public interface MatchingProfessionnelService {

    /**
     * Recherche les professionnels (Avocats) qualifiés pour une spécialité juridique donnée.
     */
    List<Professionnel> matcherParSpecialite(Specialite specialite, int limite);

    /**
     * Recherche les professionnels (Psychologues) qualifiés pour une catégorie de besoin psychologique.
     */
    List<Professionnel> matcherParCategorieBesoin(CategorieBesoin categorieBesoin, int limite);
}
