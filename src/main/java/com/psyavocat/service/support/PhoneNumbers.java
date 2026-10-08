package com.psyavocat.service.support;

/**
 * Règle unique de validité d'un numéro de téléphone (partagée par tous les profils).
 *
 * Format accepté : indicatif international facultatif (+223…), puis 8 à 15 chiffres
 * (longueur maximale E.164). Espaces, tirets, points et parenthèses sont ignorés.
 */
public final class PhoneNumbers {

    /** Message d'erreur commun (repris à l'identique côté Angular et Flutter). */
    public static final String MESSAGE_INVALIDE =
            "Numéro de téléphone invalide : 8 à 15 chiffres, indicatif international facultatif (ex. +223 76 12 34 56).";

    private PhoneNumbers() {
    }

    /**
     * Forme normalisée (« +22376123456 ») utilisée pour stocker et comparer,
     * ou {@code null} si le numéro est invalide.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String compact = raw.replaceAll("[\\s.\\-()]", "");
        if (!compact.matches("\\+?[0-9]{8,15}")) {
            return null;
        }
        return compact;
    }

    public static boolean isBlank(String raw) {
        return raw == null || raw.isBlank();
    }
}
