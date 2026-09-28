package com.psyavocat.service;

import com.psyavocat.dto.dossier.RepondreSoumissionRequest;
import com.psyavocat.dto.dossier.SoumissionDossierRequest;
import com.psyavocat.dto.dossier.SoumissionDossierResponseDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion des soumissions de dossiers juridiques aux avocats.
 * Une même affaire / dossier peut être soumise à plusieurs avocats.
 * Chaque avocat répond pour sa propre soumission (ACCEPTEE ou REFUSEE), indépendamment du dossier global.
 */
public interface SoumissionDossierService {

    /**
     * Soumet un dossier juridique à un avocat spécifié.
     */
    SoumissionDossierResponseDTO soumettreDossier(String dossierId, SoumissionDossierRequest request);

    /**
     * Récupère toutes les soumissions adressées à l'avocat connecté, avec filtrage optionnel par statut.
     */
    List<SoumissionDossierResponseDTO> getSoumissionsPourAvocat(String statut);

    /**
     * Permet à un avocat d'accepter ou refuser une soumission spécifique de dossier avec proposition tarifaire.
     */
    SoumissionDossierResponseDTO repondreSoumission(String soumissionId, RepondreSoumissionRequest request);

    /**
     * Récupère les soumissions associées à un dossier juridique donné.
     */
    List<SoumissionDossierResponseDTO> getSoumissionsParDossier(String dossierId);
}
