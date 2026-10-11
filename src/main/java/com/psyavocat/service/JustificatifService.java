package com.psyavocat.service;

import com.psyavocat.dto.justificatif.JustificatifResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface JustificatifService {
    JustificatifResponseDTO uploadJustificatif(MultipartFile file, String typeDocument);
    List<JustificatifResponseDTO> getMyJustificatifs();
    List<JustificatifResponseDTO> getJustificatifsByProfessionnel(String professionnelId);
    Resource downloadJustificatif(String id);
    String getPresignedUrl(String id);
    void deleteJustificatif(String id);

    /**
     * Décision de l'administration sur un justificatif : VALIDE ou REJETE (motif obligatoire).
     * Un justificatif validé n'est plus supprimable par le professionnel.
     */
    com.psyavocat.dto.justificatif.JustificatifResponseDTO decider(String id, String statut, String motif);
}
