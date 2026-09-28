package com.psyavocat.service;

import com.psyavocat.dto.rendezvous.CreateRendezVousAvocatRequest;
import com.psyavocat.dto.rendezvous.CreateRendezVousPsyRequest;
import com.psyavocat.dto.rendezvous.RendezVousResponseDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion des rendez-vous.
 *
 * RÈGLE MÉTIER OFFICIELLE PSYAVOCAT :
 * - Prise directe du rendez-vous à partir d'un créneau disponible (Disponibilite 'LIBRE').
 * - AUCUNE étape de "demande de rendez-vous en attente de validation manuelle par le praticien".
 * - Calcul et simulation du paiement d'un acompte obligatoire de 20 %.
 * - Le rendez-vous passe immédiatement à l'état 'CONFIRME'.
 * - Annulation possible par le patient ou le professionnel.
 */
public interface RendezVousService {

    /**
     * Réservation directe d'un rendez-vous avec un psychologue.
     */
    RendezVousResponseDTO createRendezVousPsychologue(CreateRendezVousPsyRequest request);

    /**
     * Réservation directe d'un rendez-vous avec un avocat suite à l'acceptation d'une soumission de dossier.
     */
    RendezVousResponseDTO createRendezVousAvocat(CreateRendezVousAvocatRequest request);

    /**
     * Récupère la liste des rendez-vous de l'utilisateur authentifié (patient ou professionnel).
     */
    List<RendezVousResponseDTO> getMyRendezVous();

    /**
     * Annule un rendez-vous existant. L'appelant doit être le patient ou le praticien concerné.
     */
    RendezVousResponseDTO annulerRendezVous(String id);
}
