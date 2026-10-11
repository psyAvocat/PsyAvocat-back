package com.psyavocat.service;

import com.psyavocat.dto.dossier.PieceJointeDTO;

import com.psyavocat.dto.dossier.CreateDossierRequest;
import com.psyavocat.dto.dossier.DossierResponseDTO;
import com.psyavocat.dto.dossier.RepondreSoumissionRequest;
import com.psyavocat.dto.dossier.SoumissionDossierRequest;
import com.psyavocat.dto.dossier.SoumissionDossierResponseDTO;

import java.util.List;

/**
 * Contrat de service pour la gestion des dossiers juridiques.
 */
public interface DossierService {

    DossierResponseDTO createDossier(CreateDossierRequest request);

    List<DossierResponseDTO> getMyDossiers();

    DossierResponseDTO getDossierById(String id);

    SoumissionDossierResponseDTO soumettreDossier(String dossierId, SoumissionDossierRequest request);

    List<SoumissionDossierResponseDTO> getSoumissionsPourAvocat(String statut);

    SoumissionDossierResponseDTO repondreSoumission(String soumissionId, RepondreSoumissionRequest request);

    /** Ajoute une pièce jointe (bucket privé R2) au dossier du justiciable connecté. */
    PieceJointeDTO ajouterPieceJointe(String dossierId, org.springframework.web.multipart.MultipartFile fichier);

    /** Retire une pièce jointe tant que le dossier n'est pas pris en charge. */
    void supprimerPieceJointe(String dossierId, String pieceJointeId);
}
