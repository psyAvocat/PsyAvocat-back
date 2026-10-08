package com.psyavocat.mapper;

import com.psyavocat.entity.PhotoProfil;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.service.support.ClientAccounts;
import com.psyavocat.storage.service.ImageStorageService;
import org.springframework.stereotype.Component;

/**
 * Calcule l'URL d'affichage d'une photo de profil au moment de la lecture.
 *
 * Le bucket R2 {@code psyavocat-media} est privé : seule la clé d'objet est durable,
 * l'URL présignée est générée à chaque réponse pour ne jamais être expirée.
 */
@Component
public class MediaUrlResolver {

    private final ImageStorageService imageStorageService;

    public MediaUrlResolver(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    /** URL de la photo d'un utilisateur (professionnel ou client), ou {@code null}. */
    public String photoUrlOf(Utilisateur utilisateur) {
        if (utilisateur instanceof Professionnel pro) {
            String fresh = imageStorageService.getAccessUrl(pro.getPhotoObjectKey());
            // Repli : ancienne URL enregistrée (comptes créés avant la génération à la lecture).
            return fresh != null ? fresh : pro.getPhotoUrl();
        }
        PhotoProfil photo = ClientAccounts.photoOf(utilisateur);
        return photo != null ? imageStorageService.getAccessUrl(photo.getObjectKey()) : null;
    }
}
