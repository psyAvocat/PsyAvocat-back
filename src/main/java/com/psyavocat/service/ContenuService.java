package com.psyavocat.service;

import com.psyavocat.dto.contenu.ContenuRequest;
import com.psyavocat.dto.contenu.ContenuResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Publications des professionnels.
 *
 * Règle absolue (appliquée ici, impossible à contourner par un client) :
 * seul un AVOCAT publie un ARTICLE, seul un PSYCHOLOGUE publie un CONSEIL.
 */
public interface ContenuService {

    /**
     * Contenus publiés d'un type.
     * @param tri « recent » (par défaut) ou « populaire » (nombre de consultations)
     */
    List<ContenuResponseDTO> rechercher(String type, String q, String specialiteId, String tri);

    /** Détail d'un contenu publié (404 s'il est désactivé ou supprimé, sauf pour son auteur). */
    ContenuResponseDTO getContenu(String id);

    List<ContenuResponseDTO> getMesContenus();

    ContenuResponseDTO creer(ContenuRequest request);

    ContenuResponseDTO modifier(String id, ContenuRequest request);

    ContenuResponseDTO changerStatut(String id, boolean actif);

    ContenuResponseDTO televerserImage(String id, MultipartFile image);

    void supprimer(String id);
}
