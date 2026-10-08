package com.psyavocat.service.support;

import com.psyavocat.entity.Client;
import com.psyavocat.entity.Justiciable;
import com.psyavocat.entity.Patient;
import com.psyavocat.entity.PhotoProfil;
import com.psyavocat.entity.Utilisateur;

/**
 * Règles d'identification des comptes clients (application mobile).
 *
 * Un client est un {@link Client} (profil unifié valable dans les deux univers)
 * ou un ancien profil {@link Patient} / {@link Justiciable}.
 */
public final class ClientAccounts {

    private ClientAccounts() {
    }

    public static boolean isClient(Utilisateur utilisateur) {
        return utilisateur instanceof Client
                || utilisateur instanceof Patient
                || utilisateur instanceof Justiciable;
    }

    /** Photo de profil d'un client, ou {@code null} si l'utilisateur n'est pas un client. */
    public static PhotoProfil photoOf(Utilisateur utilisateur) {
        PhotoProfil photo = null;
        if (utilisateur instanceof Client client) {
            photo = client.getPhoto();
            if (photo == null) {
                photo = new PhotoProfil();
                client.setPhoto(photo);
            }
        } else if (utilisateur instanceof Patient patient) {
            photo = patient.getPhoto();
            if (photo == null) {
                photo = new PhotoProfil();
                patient.setPhoto(photo);
            }
        } else if (utilisateur instanceof Justiciable justiciable) {
            photo = justiciable.getPhoto();
            if (photo == null) {
                photo = new PhotoProfil();
                justiciable.setPhoto(photo);
            }
        }
        return photo;
    }
}
