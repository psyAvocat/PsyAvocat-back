package com.psyavocat.service;

import com.psyavocat.dto.referentiel.CategorieBesoinDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion des référentiels (Spécialités et Catégories de besoin).
 */
public interface ReferentielService {

    List<SpecialiteDTO> getAllSpecialites();

    SpecialiteDTO createSpecialite(SpecialiteDTO dto);

    List<CategorieBesoinDTO> getCategoriesBesoin(String typeProfessionnel);

    CategorieBesoinDTO createCategorieBesoin(CategorieBesoinDTO dto);
}
