package com.psyavocat.service;

import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion et la recherche des professionnels (Avocats et Psychologues).
 */
public interface ProfessionnelService {

    /**
     * Recherche publique : professionnels validés et actifs d'un type,
     * avec recherche texte facultative (nom, prénom, ville, spécialité).
     */
    List<ProfessionnelResponseDTO> rechercherPublic(String type, String q, String ville,
                                                    String specialiteId, String modeConsultation);

    /** Fiche publique : 404 si le professionnel n'est pas validé ou plus actif. */
    ProfessionnelResponseDTO getProfessionnelPublic(String id);

    List<ProfessionnelResponseDTO> searchProfessionnels(
            String type,
            String ville,
            String specialiteId,
            String modeConsultation
    );

    ProfessionnelResponseDTO getProfessionnelById(String id);

    List<ProfessionnelResponseDTO> getProfessionnelsEnAttente();

    ProfessionnelResponseDTO updateStatutValidation(String id, String nouveauStatut);
    ProfessionnelResponseDTO updateStatutValidation(String id, String nouveauStatut, String motif);
}

