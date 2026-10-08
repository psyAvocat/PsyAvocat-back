package com.psyavocat.entity;

/**
 * Motifs autorisés pour signaler un professionnel (liste contrôlée côté serveur).
 * Le libellé est stocké dans {@link Signalement#getMotif()} pour l'affichage Admin.
 */
public enum MotifSignalement {
    MANQUEMENT_DEONTOLOGIQUE("Manquement déontologique"),
    COMPORTEMENT_INAPPROPRIE("Comportement inapproprié"),
    INFORMATIONS_TROMPEUSES("Informations de profil trompeuses"),
    USURPATION_IDENTITE("Usurpation d'identité ou de titre"),
    RENDEZ_VOUS_NON_HONORE("Rendez-vous non honoré"),
    AUTRE("Autre");

    private final String libelle;

    MotifSignalement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
