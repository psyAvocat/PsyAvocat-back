package com.psyavocat.service;

import com.psyavocat.dto.disponibilite.CreateDisponibiliteRequest;
import com.psyavocat.dto.disponibilite.DisponibiliteResponseDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion des créneaux de disponibilité des professionnels.
 */
public interface DisponibiliteService {

    DisponibiliteResponseDTO createDisponibilite(CreateDisponibiliteRequest request);

    List<DisponibiliteResponseDTO> getMyDisponibilites();

    void deleteDisponibilite(String id);

    List<DisponibiliteResponseDTO> getDisponibilitesLibres(String professionnelId);
}
