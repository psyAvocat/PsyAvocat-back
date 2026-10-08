package com.psyavocat.service;

import com.psyavocat.dto.disponibilite.CreateDisponibiliteRequest;
import com.psyavocat.dto.disponibilite.DisponibiliteResponseDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion des créneaux de disponibilité des professionnels.
 */
public interface DisponibiliteService {

    DisponibiliteResponseDTO createDisponibilite(CreateDisponibiliteRequest request);

    List<DisponibiliteResponseDTO> createDisponibilitesBatch(List<CreateDisponibiliteRequest> requests);

    List<DisponibiliteResponseDTO> getMyDisponibilites();

    void deleteDisponibilite(String id);

    List<DisponibiliteResponseDTO> getDisponibilitesLibres(String professionnelId);

    /**
     * Créneaux à venir d'un professionnel, libres ET réservés (pour la légende du calendrier).
     * Aucune information sur le client d'un créneau réservé n'est exposée.
     */
    List<DisponibiliteResponseDTO> getCreneauxAVenir(String professionnelId);
}
