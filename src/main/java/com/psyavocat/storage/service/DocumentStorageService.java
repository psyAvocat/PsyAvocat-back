package com.psyavocat.storage.service;

import com.psyavocat.storage.model.StoredDocument;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * Abstraction de stockage des documents sensibles et privés (justificatifs, diplômes, agréments).
 * Le bucket reste strictement privé : l'accès se fait soit par URL présignée à courte durée de vie,
 * soit par téléchargement contrôlé en flux sécurisé.
 */
public interface DocumentStorageService {

    /**
     * Valide et téléverse un document privé vers le stockage objet sécurisé.
     * @param file fichier document reçu
     * @param cleObjet clé générée de manière sécurisée par le backend
     * @return métadonnées du document stocké
     */
    StoredDocument uploadDocument(MultipartFile file, String cleObjet);

    /**
     * Télécharge le flux d'un document stocké pour le servir sous forme de Resource Spring.
     * @param cleObjet clé objet dans le bucket
     * @param nomFichierOriginal nom pour le Content-Disposition
     * @return ressource téléchargeable
     */
    Resource downloadDocument(String cleObjet, String nomFichierOriginal);

    /**
     * Génère une URL présignée temporaire (durée de vie courte) pour consultation directe sécurisée.
     * @param cleObjet clé de l'objet dans le bucket privé
     * @param duree durée de validité (ex: 15 minutes)
     * @return URL présignée signée cryptographiquement
     */
    String generatePresignedDownloadUrl(String cleObjet, Duration duree);

    /**
     * Supprime définitivement un document du stockage.
     * @param cleObjet clé objet dans le bucket
     */
    void deleteDocument(String cleObjet);

    /**
     * Indique si le bucket et les credentials sont configurés.
     */
    boolean isAvailable();
}
