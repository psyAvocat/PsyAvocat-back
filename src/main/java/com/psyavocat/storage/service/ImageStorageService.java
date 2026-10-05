package com.psyavocat.storage.service;

import com.psyavocat.storage.model.StoredImage;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction de stockage des images et médias (photos de profil, avatars).
 * Aucune dépendance directe au SDK de stockage dans les contrôleurs.
 */
public interface ImageStorageService {

    /**
     * Valide et téléverse une image vers le stockage externe.
     * @param file fichier image reçu
     * @param dossier dossier logique (ex: "psyavocat/avatars")
     * @param idPrefix préfixe pour le nommage sécurisé généré par le backend
     * @return métadonnées de l'image stockée
     */
    StoredImage uploadImage(MultipartFile file, String dossier, String idPrefix);

    /**
     * Supprime une image par son identifiant unique de stockage.
     * @param publicId identifiant de l'image
     */
    void deleteImage(String publicId);

    /**
     * Indique si le service est configuré et disponible.
     */
    boolean isAvailable();
}
