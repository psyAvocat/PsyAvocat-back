package com.psyavocat.service.support;

import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.Specialite;
import com.psyavocat.exception.BadRequestException;

import java.util.Set;

/**
 * Types de professionnel (univers) des référentiels : AVOCAT ou PSYCHOLOGUE.
 *
 * Le type est TOUJOURS une donnée enregistrée et validée (domaine, spécialité, catégorie de besoin),
 * jamais déduit d'un libellé.
 */
public final class TypesProfessionnel {

    public static final String AVOCAT = "AVOCAT";
    public static final String PSYCHOLOGUE = "PSYCHOLOGUE";
    private static final Set<String> VALEURS = Set.of(AVOCAT, PSYCHOLOGUE);

    private TypesProfessionnel() {
    }

    /** Valeur normalisée, ou {@code null} si absente. Lève 400 si la valeur est inconnue. */
    public static String normaliser(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String normalise = valeur.trim().toUpperCase();
        if (!VALEURS.contains(normalise)) {
            throw new BadRequestException("Type de professionnel invalide (AVOCAT ou PSYCHOLOGUE) : " + valeur);
        }
        return normalise;
    }

    /** Valeur normalisée, ou {@code null} si absente OU inconnue (lecture tolérante de données existantes). */
    public static String normaliserSansErreur(String valeur) {
        if (valeur == null) {
            return null;
        }
        String normalise = valeur.trim().toUpperCase();
        return VALEURS.contains(normalise) ? normalise : null;
    }

    /** Valeur normalisée obligatoire. */
    public static String exiger(String valeur, String libelle) {
        String normalise = normaliser(valeur);
        if (normalise == null) {
            throw new BadRequestException(libelle + " : le type de professionnel (AVOCAT ou PSYCHOLOGUE) est obligatoire.");
        }
        return normalise;
    }

    /** Type enregistré d'une spécialité (null si non qualifiée). */
    public static String typeDe(Specialite specialite) {
        if (specialite == null || specialite.getTypeProfessionnel() == null || specialite.getTypeProfessionnel().isBlank()) {
            return null;
        }
        return specialite.getTypeProfessionnel().trim().toUpperCase();
    }

    /** Type d'un professionnel selon son métier. */
    public static String typeDe(Professionnel professionnel) {
        if (professionnel instanceof Avocat) {
            return AVOCAT;
        }
        if (professionnel instanceof Psychologue) {
            return PSYCHOLOGUE;
        }
        return null;
    }
}
