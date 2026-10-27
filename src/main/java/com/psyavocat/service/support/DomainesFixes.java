package com.psyavocat.service.support;

/**
 * Les deux seuls domaines de l'application, un par univers. Ils sont créés automatiquement
 * au démarrage (voir {@code DomainesFixesInitializer}) et ne sont ni créés ni supprimés par
 * l'administration : chaque spécialité est rattachée au domaine de son univers.
 */
public enum DomainesFixes {

    PSYCHOLOGIQUE("PSYCHOLOGIQUE", "Psychologique", TypesProfessionnel.PSYCHOLOGUE,
            "Accompagnement psychologique : spécialités des psychologues."),
    JURIDIQUE("JURIDIQUE", "Juridique", TypesProfessionnel.AVOCAT,
            "Conseil et défense juridiques : spécialités des avocats.");

    private final String code;
    private final String nom;
    private final String typeProfessionnel;
    private final String descriptionParDefaut;

    DomainesFixes(String code, String nom, String typeProfessionnel, String descriptionParDefaut) {
        this.code = code;
        this.nom = nom;
        this.typeProfessionnel = typeProfessionnel;
        this.descriptionParDefaut = descriptionParDefaut;
    }

    public String code() {
        return code;
    }

    public String nom() {
        return nom;
    }

    public String typeProfessionnel() {
        return typeProfessionnel;
    }

    public String descriptionParDefaut() {
        return descriptionParDefaut;
    }

    /** Domaine de l'univers donné (AVOCAT ou PSYCHOLOGUE), ou null si le type est inconnu. */
    public static DomainesFixes pourType(String typeProfessionnel) {
        for (DomainesFixes d : values()) {
            if (d.typeProfessionnel.equals(typeProfessionnel)) {
                return d;
            }
        }
        return null;
    }

    /** Vrai si le code est celui d'un domaine fixe (faux pour null). */
    public static boolean estCodeFixe(String code) {
        return code != null && (PSYCHOLOGIQUE.code.equals(code) || JURIDIQUE.code.equals(code));
    }
}
