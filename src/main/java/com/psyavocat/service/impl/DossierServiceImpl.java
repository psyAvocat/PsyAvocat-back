package com.psyavocat.service.impl;

import com.psyavocat.dto.dossier.*;
import com.psyavocat.entity.Dossier;
import com.psyavocat.entity.PieceJointe;
import com.psyavocat.entity.Specialite;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.DossierRepository;
import com.psyavocat.repository.PieceJointeRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.DossierService;
import com.psyavocat.service.SoumissionDossierService;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.service.support.AccesProfessionnel;
import com.psyavocat.service.support.ClientAccounts;
import com.psyavocat.storage.model.StoredDocument;
import com.psyavocat.storage.service.DocumentStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class DossierServiceImpl implements DossierService {

    /** Nombre maximal de pièces jointes par dossier. */
    static final int MAX_PIECES_JOINTES = 10;
    private static final Duration DUREE_LIEN_DOCUMENT = Duration.ofMinutes(15);
    private static final String DOSSIER_PRIS_EN_CHARGE = "PRIS_EN_CHARGE";

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SoumissionDossierService soumissionDossierService;
    private final AuthenticationContext authenticationContext;
    private final AccesProfessionnel accesProfessionnel;
    private final PieceJointeRepository pieceJointeRepository;
    private final SpecialiteRepository specialiteRepository;
    private final DocumentStorageService documentStorageService;

    public DossierServiceImpl(
            DossierRepository dossierRepository,
            UtilisateurRepository utilisateurRepository,
            SoumissionDossierService soumissionDossierService,
            AuthenticationContext authenticationContext,
            AccesProfessionnel accesProfessionnel,
            PieceJointeRepository pieceJointeRepository,
            SpecialiteRepository specialiteRepository,
            DocumentStorageService documentStorageService
    ) {
        this.dossierRepository = dossierRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.soumissionDossierService = soumissionDossierService;
        this.authenticationContext = authenticationContext;
        this.accesProfessionnel = accesProfessionnel;
        this.pieceJointeRepository = pieceJointeRepository;
        this.specialiteRepository = specialiteRepository;
        this.documentStorageService = documentStorageService;
    }

    @Override
    public DossierResponseDTO createDossier(CreateDossierRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur justiciable = utilisateurRepository.findById(uid)
                .filter(ClientAccounts::isClient)
                .orElseThrow(() -> new ForbiddenException("Seul un client peut créer un dossier juridique"));

        Dossier dossier = new Dossier();
        dossier.setTitre(request.getTitre().trim());
        dossier.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        dossier.setDateOuverture(LocalDate.now());
        dossier.setStatut("OUVERT");
        dossier.setJusticiable(justiciable);
        dossier.setSpecialite(domaineJuridique(request.getSpecialiteId()));

        Dossier saved = dossierRepository.save(dossier);
        return toDossierDto(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DossierResponseDTO> getMyDossiers() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return dossierRepository.findByJusticiableIdOrderByDateOuvertureDesc(uid).stream()
                .map(d -> toDossierDto(d, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DossierResponseDTO getDossierById(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        boolean isOwner = dossier.getJusticiable() != null && dossier.getJusticiable().getId().equals(uid);
        boolean isSubmittedAvocatActif = estAvocatDestinataireActif(dossier, uid);

        if (!isOwner && !isSubmittedAvocatActif) {
            throw new ForbiddenException("Accès non autorisé : ce dossier a été confié à un autre confrère ou n'est plus accessible");
        }
        if (isOwner) {
            return toDossierDto(dossier, null);
        }

        // Avocat destinataire : compte approuvé obligatoire, et seule SA soumission est visible
        // (jamais les réponses ni les tarifs proposés par ses confrères).
        Professionnel avocat = accesProfessionnel.exigerAvocatApprouve();
        return toDossierDto(dossier, avocat.getId());
    }

    @Override
    public SoumissionDossierResponseDTO soumettreDossier(String dossierId, SoumissionDossierRequest request) {
        return soumissionDossierService.soumettreDossier(dossierId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoumissionDossierResponseDTO> getSoumissionsPourAvocat(String statut) {
        return soumissionDossierService.getSoumissionsPourAvocat(statut);
    }

    @Override
    public SoumissionDossierResponseDTO repondreSoumission(String soumissionId, RepondreSoumissionRequest request) {
        return soumissionDossierService.repondreSoumission(soumissionId, request);
    }

    // ------------------------------------------------------------------
    // Pièces jointes (bucket privé psyavocat-documents)
    // ------------------------------------------------------------------

    @Override
    public PieceJointeDTO ajouterPieceJointe(String dossierId, MultipartFile fichier) {
        Dossier dossier = dossierDuProprietaire(dossierId);
        if (pieceJointeRepository.findByDossierId(dossier.getId()).size() >= MAX_PIECES_JOINTES) {
            throw new BadRequestException("Un dossier ne peut pas contenir plus de " + MAX_PIECES_JOINTES + " pièces jointes.");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new BadRequestException("Fichier manquant ou vide.");
        }

        String nomOriginal = StringUtils.cleanPath(fichier.getOriginalFilename() != null ? fichier.getOriginalFilename() : "document");
        if (nomOriginal.contains("..")) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
        int point = nomOriginal.lastIndexOf('.');
        String extension = point > 0 ? nomOriginal.substring(point).toLowerCase() : "";
        // Clé générée par le serveur : aucun chemin fourni par le client n'est utilisé.
        String cleObjet = "dossiers/" + dossier.getId() + "/" + UUID.randomUUID() + extension;

        // Le service de stockage valide le type MIME, l'extension et la taille, et échoue
        // explicitement (503) si R2 n'est pas configuré : aucun stockage de repli local.
        StoredDocument stocke = documentStorageService.uploadDocument(fichier, cleObjet);

        PieceJointe pj = new PieceJointe();
        pj.setNom(nomOriginal);
        pj.setCleObjet(stocke.getCleObjet());
        pj.setUrl("r2://" + stocke.getCleObjet());
        pj.setTypeMime(fichier.getContentType());
        pj.setTailleOctets(fichier.getSize());
        pj.setDateAjout(LocalDate.now());
        pj.setDossier(dossier);
        PieceJointe saved = pieceJointeRepository.save(pj);
        return toPieceJointeDto(saved);
    }

    @Override
    public void supprimerPieceJointe(String dossierId, String pieceJointeId) {
        Dossier dossier = dossierDuProprietaire(dossierId);
        if (DOSSIER_PRIS_EN_CHARGE.equalsIgnoreCase(dossier.getStatut())) {
            throw new BadRequestException("Le dossier est pris en charge par un avocat : ses pièces ne peuvent plus être retirées.");
        }
        PieceJointe pj = pieceJointeRepository.findById(pieceJointeId)
                .filter(p -> p.getDossier() != null && dossier.getId().equals(p.getDossier().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable"));

        if (pj.getCleObjet() != null) {
            documentStorageService.deleteDocument(pj.getCleObjet());
        }
        dossier.getPiecesJointes().remove(pj);
        pieceJointeRepository.delete(pj);
    }

    // ------------------------------------------------------------------
    // Règles
    // ------------------------------------------------------------------

    private Dossier dossierDuProprietaire(String dossierId) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));
        if (dossier.getJusticiable() == null || !dossier.getJusticiable().getId().equals(uid)) {
            throw new ForbiddenException("Seul le propriétaire du dossier peut gérer ses pièces jointes");
        }
        return dossier;
    }

    private boolean estAvocatDestinataireActif(Dossier dossier, String uid) {
        return dossier.getSoumissions() != null && dossier.getSoumissions().stream()
                .anyMatch(s -> s.getAvocat() != null
                        && s.getAvocat().getId().equals(uid)
                        && !"CADUQUE".equalsIgnoreCase(s.getStatut())
                        && !"REFUSEE".equalsIgnoreCase(s.getStatut()));
    }

    /** Domaine juridique facultatif : doit être une spécialité de l'univers Avocat. */
    private Specialite domaineJuridique(String specialiteId) {
        if (specialiteId == null || specialiteId.isBlank()) {
            return null;
        }
        Specialite specialite = specialiteRepository.findById(specialiteId)
                .orElseThrow(() -> new BadRequestException("Domaine juridique inconnu"));
        if (!com.psyavocat.service.support.TypesProfessionnel.AVOCAT.equals(
                com.psyavocat.service.support.TypesProfessionnel.typeDe(specialite))) {
            throw new BadRequestException("Ce domaine n'appartient pas à l'univers juridique");
        }
        return specialite;
    }

    private PieceJointeDTO toPieceJointeDto(PieceJointe pj) {
        return PieceJointeDTO.builder()
                .id(pj.getId())
                .nom(pj.getNom())
                .url(lienTemporaire(pj))
                .dateAjout(pj.getDateAjout())
                .typeMime(pj.getTypeMime())
                .tailleOctets(pj.getTailleOctets())
                .build();
    }

    /** Lien présigné de courte durée, généré à chaque lecture par un utilisateur autorisé. */
    private String lienTemporaire(PieceJointe pj) {
        if (pj.getCleObjet() == null || !documentStorageService.isAvailable()) {
            return null;
        }
        try {
            return documentStorageService.generatePresignedDownloadUrl(pj.getCleObjet(), DUREE_LIEN_DOCUMENT);
        } catch (Exception e) {
            log.warn("Lien temporaire indisponible pour la pièce jointe {} : {}", pj.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * @param avocatLecteurId si renseigné, seules les soumissions adressées à cet avocat sont incluses.
     */
    private DossierResponseDTO toDossierDto(Dossier d, String avocatLecteurId) {
        List<PieceJointeDTO> pjDtos = d.getPiecesJointes() != null
                ? d.getPiecesJointes().stream().map(this::toPieceJointeDto).toList()
                : Collections.emptyList();

        List<EcheanceDTO> echDtos = d.getEcheances() != null
                ? d.getEcheances().stream()
                    .map(ech -> new EcheanceDTO(ech.getId(), ech.getDescription(), ech.getDate(), ech.getStatut()))
                    .toList()
                : Collections.emptyList();

        List<SoumissionDossierResponseDTO> soumDtos = d.getId() != null
                ? soumissionDossierService.getSoumissionsParDossier(d.getId())
                : Collections.emptyList();
        if (avocatLecteurId != null) {
            soumDtos = soumDtos.stream().filter(s -> avocatLecteurId.equals(s.getAvocatId())).toList();
        }

        return DossierResponseDTO.builder()
                .id(d.getId())
                .titre(d.getTitre())
                .description(d.getDescription())
                .dateOuverture(d.getDateOuverture())
                .statut(d.getStatut())
                .specialiteId(d.getSpecialite() != null ? d.getSpecialite().getId() : null)
                .specialiteNom(d.getSpecialite() != null ? d.getSpecialite().getNom() : null)
                .justiciableId(d.getJusticiable() != null ? d.getJusticiable().getId() : null)
                .piecesJointes(pjDtos)
                .echeances(echDtos)
                .soumissions(soumDtos)
                .build();
    }
}
