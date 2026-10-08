package com.psyavocat.dto.signalement;

import java.time.LocalDateTime;

/** Confirmation renvoyée à l'auteur d'un signalement (sans donnée sur le traitement Admin). */
public record SignalementCreeDTO(String id, String statut, LocalDateTime dateSignalement) {
}
