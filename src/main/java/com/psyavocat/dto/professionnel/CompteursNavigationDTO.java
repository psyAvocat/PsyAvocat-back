package com.psyavocat.dto.professionnel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nombres affichés en badge dans la barre latérale de l'espace professionnel. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteursNavigationDTO {
    /** Nouveaux dossiers reçus, pas encore vus (0 pour un psychologue). Remis à zéro à l'ouverture de la page Dossiers. */
    private long dossiersNouveaux;
    /** Événements de rendez-vous (réservation, annulation, déplacement…) non encore vus. */
    private long rendezVousNouveaux;
    /** Messages reçus non lus. */
    private long messagesNonLus;
}
