package com.psyavocat.service;

import com.psyavocat.dto.referentiel.CategorieBesoinDTO;
import com.psyavocat.dto.referentiel.DomaineDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion des référentiels (Domaines, Spécialités et Catégories de besoin).
 */
public interface ReferentielService {

    List<DomaineDTO> getAllDomaines();

    DomaineDTO createDomaine(DomaineDTO dto);

    DomaineDTO updateDomaine(String id, DomaineDTO dto);

    void deleteDomaine(String id);

    List<SpecialiteDTO> getAllSpecialites();

    SpecialiteDTO createSpecialite(SpecialiteDTO dto);

    SpecialiteDTO updateSpecialite(String id, SpecialiteDTO dto);

    void deleteSpecialite(String id);

    List<CategorieBesoinDTO> getCategoriesBesoin(String typeProfessionnel);

    List<CategorieBesoinDTO> getCategoriesBesoin(String typeProfessionnel, boolean includeInactive);

    CategorieBesoinDTO createCategorieBesoin(CategorieBesoinDTO dto);

    CategorieBesoinDTO updateCategorieBesoin(String id, CategorieBesoinDTO dto);

    CategorieBesoinDTO toggleActifCategorieBesoin(String id);

    void deleteCategorieBesoin(String id);
}
