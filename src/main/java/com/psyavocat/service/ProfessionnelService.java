package com.psyavocat.service;

import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion et la recherche des professionnels (Avocats et Psychologues).
 */
public interface ProfessionnelService {

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

