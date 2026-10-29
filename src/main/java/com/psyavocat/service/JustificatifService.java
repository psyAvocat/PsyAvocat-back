package com.psyavocat.service;

import com.psyavocat.dto.justificatif.JustificatifResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface JustificatifService {
    /**
     * @param remplaceId justificatif complémentaire à mettre à jour (null : nouveau dépôt) ; une version
     *                   validée reste en vigueur jusqu'à la décision, un dépôt non validé est remplacé aussitôt
     */
    JustificatifResponseDTO uploadJustificatif(MultipartFile file, String typeDocument, String remplaceId);
    List<JustificatifResponseDTO> getMyJustificatifs();
    List<JustificatifResponseDTO> getJustificatifsByProfessionnel(String professionnelId);

    /** Documents déposés par des comptes déjà validés (mises à jour), en attente de vérification. */
    List<JustificatifResponseDTO> getMisesAJourEnAttente();
    Resource downloadJustificatif(String id);
    String getPresignedUrl(String id);
    void deleteJustificatif(String id);

    /**
     * Décision de l'administration sur un justificatif : VALIDE ou REJETE (motif obligatoire).
     * Un justificatif validé n'est plus supprimable par le professionnel.
     */
    com.psyavocat.dto.justificatif.JustificatifResponseDTO decider(String id, String statut, String motif);
}
