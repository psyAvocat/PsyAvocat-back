package com.psyavocat.service.impl;

import com.psyavocat.dto.justificatif.JustificatifResponseDTO;
import com.psyavocat.entity.JustificatifProfessionnel;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.JustificatifProfessionnelRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.JustificatifService;
import com.psyavocat.service.support.AccesProfessionnel;
import com.psyavocat.storage.model.StoredDocument;
import com.psyavocat.storage.service.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class JustificatifServiceImpl implements JustificatifService {

    private static final Logger log = LoggerFactory.getLogger(JustificatifServiceImpl.class);

    private final JustificatifProfessionnelRepository justificatifRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;
    private final DocumentStorageService documentStorageService;
    private final AccesProfessionnel accesProfessionnel;
    private final com.psyavocat.service.NotificationService notificationService;
    private final com.psyavocat.service.support.JournalDecisions journalDecisions;
    private final com.psyavocat.repository.AdministrateurRepository administrateurRepository;
    private final Path fileStorageLocation;

    public JustificatifServiceImpl(
            JustificatifProfessionnelRepository justificatifRepository,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext,
            DocumentStorageService documentStorageService,
            AccesProfessionnel accesProfessionnel,
            com.psyavocat.service.NotificationService notificationService,
            com.psyavocat.service.support.JournalDecisions journalDecisions,
            com.psyavocat.repository.AdministrateurRepository administrateurRepository
    ) {
        this.justificatifRepository = justificatifRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
        this.documentStorageService = documentStorageService;
        this.accesProfessionnel = accesProfessionnel;
        this.notificationService = notificationService;
        this.journalDecisions = journalDecisions;
        this.administrateurRepository = administrateurRepository;
        this.fileStorageLocation = Paths.get("uploads/justificatifs").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.warn("Impossible d'initialiser le répertoire local de justificatifs : {}", ex.getMessage());
        }
    }

    @Override
    public JustificatifResponseDTO uploadJustificatif(MultipartFile file, String typeDocument, String remplaceId) {
        // Dépôt ouvert aux comptes en attente de vérification et approuvés uniquement.
        Professionnel professionnel = accesProfessionnel.exigerParcoursVerificationOuvert();

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier manquant ou vide.");
        }

        // Mise à jour d'un justificatif complémentaire précis (plusieurs partagent le même type).
        JustificatifProfessionnel cible = null;
        if (remplaceId != null && !remplaceId.isBlank()) {
            cible = justificatifRepository.findById(remplaceId)
                    .filter(j -> j.getProfessionnel().getId().equals(professionnel.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Justificatif à mettre à jour introuvable."));
            if (estDocumentUnique(cible.getTypeDocument())) {
                throw new BadRequestException("Ce document se met à jour depuis son emplacement dans votre profil.");
            }
            typeDocument = cible.getTypeDocument();
        }
        boolean cibleValidee = cible != null && "VALIDE".equalsIgnoreCase(cible.getStatutValidation());

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        if (originalFileName.contains("..")) {
            throw new BadRequestException("Nom de fichier invalide (séquence de chemin non autorisée).");
        }

        String fileExtension = "";
        int extIndex = originalFileName.lastIndexOf('.');
        if (extIndex > 0) {
            fileExtension = originalFileName.substring(extIndex);
        }

        // Clé objet sécurisée générée par le backend : justificatifs/professionnel/{proId}/{uuid}.ext
        String uniqueId = UUID.randomUUID().toString();
        String cleObjet = "justificatifs/professionnel/" + professionnel.getId() + "/" + uniqueId + fileExtension;

        JustificatifProfessionnel justif = new JustificatifProfessionnel();
        justif.setNomFichier(originalFileName);
        justif.setTypeDocument(typeDocument);
        justif.setStatutValidation("EN_ATTENTE");
        justif.setDateDepot(LocalDate.now());
        justif.setProfessionnel(professionnel);
        justif.setTailleOctets(file.getSize());
        justif.setTypeMime(file.getContentType());

        if (documentStorageService.isAvailable()) {
            StoredDocument stored = documentStorageService.uploadDocument(file, cleObjet);
            justif.setCleObjet(stored.getCleObjet());
            justif.setCheminStockage("r2://" + stored.getCleObjet());
        } else {
            // Mode de secours local (si R2 non configuré en dev)
            log.warn("Cloudflare R2 non disponible, enregistrement sur disque local en mode dégradé");
            Path targetLocation = this.fileStorageLocation.resolve(uniqueId + fileExtension);
            try {
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                justif.setCheminStockage(targetLocation.toString());
            } catch (IOException ex) {
                throw new RuntimeException("Impossible d'enregistrer le fichier sur le disque : " + originalFileName, ex);
            }
        }

        // Mise à jour d'un document déjà validé : le document actuel reste valable jusqu'à la
        // décision ; une mise à jour encore en attente pour ce même type est remplacée.
        boolean miseAJour = cibleValidee || (estDocumentUnique(typeDocument)
                && !justificatifRepository.findByProfessionnelIdAndTypeDocumentAndStatutValidation(
                        professionnel.getId(), typeDocument, "VALIDE").isEmpty());
        if (miseAJour && cible == null) {
            for (JustificatifProfessionnel ancien : justificatifRepository.findByProfessionnelIdAndTypeDocumentAndStatutValidation(
                    professionnel.getId(), typeDocument, "EN_ATTENTE")) {
                supprimerFichierEtLigne(ancien);
            }
        }
        if (cibleValidee) {
            // Une seule nouvelle version à la fois : les précédentes (en attente ou refusées) sont retirées.
            justif.setRemplaceId(cible.getId());
            justificatifRepository.findByRemplaceId(cible.getId()).forEach(this::supprimerFichierEtLigne);
        } else if (cible != null) {
            // Dépôt pas encore validé (en examen ou refusé) : le nouveau fichier le remplace aussitôt.
            justif.setRemplaceId(cible.getRemplaceId());
        }

        JustificatifProfessionnel saved = justificatifRepository.save(justif);
        if (cible != null && !cibleValidee) {
            supprimerFichierEtLigne(cible);
        }
        // Compte déjà validé : l'administration vérifie tout nouveau dépôt.
        if ("APPROVED".equalsIgnoreCase(professionnel.getStatutValidation())) {
            String nom = (professionnel.getPrenom() + " " + professionnel.getNom()).trim();
            String document = cibleValidee ? cible.getNomFichier() : libelleType(typeDocument);
            String message = miseAJour
                    ? nom + " a déposé une nouvelle version de son document « " + document
                            + " ». Vérifiez-la depuis Validation des comptes."
                    : nom + " a déposé un nouveau document (« " + libelleType(typeDocument)
                            + " »). Vérifiez-le depuis Validation des comptes.";
            for (com.psyavocat.entity.Administrateur admin : administrateurRepository.findAll()) {
                notificationService.sendNotification(admin.getId(), "JUSTIFICATIF_A_VERIFIER", message, null);
            }
        }
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JustificatifResponseDTO> getMisesAJourEnAttente() {
        if (!estAdministrateur()) {
            throw new ForbiddenException("Réservé à l'administration.");
        }
        return justificatifRepository
                .findByStatutValidationAndProfessionnelStatutValidationOrderByDateDepotAsc("EN_ATTENTE", "APPROVED")
                .stream()
                .map(j -> {
                    JustificatifResponseDTO dto = mapToDto(j);
                    Professionnel p = j.getProfessionnel();
                    dto.setProfessionnelNom((p.getPrenom() + " " + p.getNom()).trim());
                    dto.setTypeProfessionnel(com.psyavocat.service.support.TypesProfessionnel.typeDe(p));
                    if (!estDocumentUnique(j.getTypeDocument())) {
                        if (j.getRemplaceId() != null) {
                            justificatifRepository.findById(j.getRemplaceId()).ifPresent(actuel -> {
                                dto.setRemplaceId(actuel.getId());
                                dto.setRemplaceNomFichier(actuel.getNomFichier());
                            });
                        }
                        return dto;
                    }
                    justificatifRepository.findByProfessionnelIdAndTypeDocumentAndStatutValidation(
                                    p.getId(), j.getTypeDocument(), "VALIDE").stream().findFirst()
                            .ifPresent(actuel -> {
                                dto.setRemplaceId(actuel.getId());
                                dto.setRemplaceNomFichier(actuel.getNomFichier());
                            });
                    return dto;
                })
                .toList();
    }

    /**
     * Documents dont un professionnel n'a qu'une version en vigueur : une nouvelle version validée
     * remplace l'ancienne. Les justificatifs complémentaires (JUSTIFICATIF_PROFESSIONNEL) s'ajoutent.
     */
    private static final java.util.Set<String> DOCUMENTS_UNIQUES =
            java.util.Set.of("CV", "PIECE_IDENTITE", "CARTE_PROFESSIONNELLE", "DIPLOME_PSYCHOLOGUE", "DIPLOME");

    private static boolean estDocumentUnique(String typeDocument) {
        return typeDocument != null && DOCUMENTS_UNIQUES.contains(typeDocument.toUpperCase());
    }

    /** Libellé lisible d'un type de document (notifications). */
    private static String libelleType(String typeDocument) {
        if (typeDocument == null) {
            return "justificatif";
        }
        return switch (typeDocument.toUpperCase()) {
            case "CV" -> "CV";
            case "PIECE_IDENTITE" -> "pièce d'identité";
            case "CARTE_PROFESSIONNELLE" -> "carte professionnelle";
            case "DIPLOME_PSYCHOLOGUE", "DIPLOME" -> "diplôme";
            default -> typeDocument.toLowerCase().replace('_', ' ');
        };
    }

    /** Retire le fichier (R2 ou disque) puis la ligne en base. */
    private void supprimerFichierEtLigne(JustificatifProfessionnel justif) {
        if (justif.getCleObjet() != null && !justif.getCleObjet().isBlank() && documentStorageService.isAvailable()) {
            documentStorageService.deleteDocument(justif.getCleObjet());
        } else if (justif.getCheminStockage() != null && !justif.getCheminStockage().startsWith("r2://")) {
            try {
                Files.deleteIfExists(Paths.get(justif.getCheminStockage()));
            } catch (Exception ex) {
                log.warn("Impossible de supprimer le fichier local : {}", ex.getMessage());
            }
        }
        justificatifRepository.delete(justif);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JustificatifResponseDTO> getMyJustificatifs() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return justificatifRepository.findByProfessionnelId(uid).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JustificatifResponseDTO> getJustificatifsByProfessionnel(String professionnelId) {
        return justificatifRepository.findByProfessionnelId(professionnelId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadJustificatif(String id) {
        JustificatifProfessionnel justif = justificatifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justificatif introuvable"));

        verifierAccesDocument(justif);

        // Si stocké sur Cloudflare R2
        if (justif.getCleObjet() != null && !justif.getCleObjet().isBlank() && documentStorageService.isAvailable()) {
            return documentStorageService.downloadDocument(justif.getCleObjet(), justif.getNomFichier());
        }

        // Sinon fallback sur le stockage local (fichiers existants / mode dégradé)
        try {
            Path filePath = Paths.get(justif.getCheminStockage()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Fichier introuvable sur le disque");
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Fichier introuvable sur le disque : " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getPresignedUrl(String id) {
        JustificatifProfessionnel justif = justificatifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justificatif introuvable"));

        verifierAccesDocument(justif);

        if (justif.getCleObjet() != null && !justif.getCleObjet().isBlank() && documentStorageService.isAvailable()) {
            return documentStorageService.generatePresignedDownloadUrl(justif.getCleObjet(), Duration.ofMinutes(15));
        }

        return null;
    }

    @Override
    public void deleteJustificatif(String id) {
        JustificatifProfessionnel justif = justificatifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justificatif introuvable"));

        verifierAccesDocument(justif);
        if (!estAdministrateur()) {
            accesProfessionnel.exigerParcoursVerificationOuvert();
            // Un justificatif déjà validé par l'administration fait foi : il n'est plus retirable.
            if ("VALIDE".equalsIgnoreCase(justif.getStatutValidation())) {
                throw new ForbiddenException("Ce justificatif a été validé par l'administration et ne peut plus être supprimé.");
            }
        }

        // Suppression de R2 si présent
        if (justif.getCleObjet() != null && !justif.getCleObjet().isBlank() && documentStorageService.isAvailable()) {
            documentStorageService.deleteDocument(justif.getCleObjet());
        } else if (justif.getCheminStockage() != null && !justif.getCheminStockage().startsWith("r2://")) {
            // Suppression fichier local
            try {
                Path localPath = Paths.get(justif.getCheminStockage());
                Files.deleteIfExists(localPath);
            } catch (Exception ex) {
                log.warn("Impossible de supprimer le fichier local : {}", ex.getMessage());
            }
        }

        justificatifRepository.delete(justif);
        log.info("Justificatif supprimé avec succès : id={}", id);
    }

    private void verifierAccesDocument(JustificatifProfessionnel justif) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        if (!estAdministrateur() && !justif.getProfessionnel().getId().equals(uid)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à accéder à ce document.");
        }
    }

    @Override
    public JustificatifResponseDTO decider(String id, String statut, String motif) {
        if (!estAdministrateur()) {
            throw new ForbiddenException("Seule l'administration peut valider un justificatif.");
        }
        String decision = statut != null ? statut.trim().toUpperCase() : "";
        if (!"VALIDE".equals(decision) && !"REJETE".equals(decision)) {
            throw new BadRequestException("Décision invalide (VALIDE ou REJETE).");
        }
        String motifNettoye = motif != null && !motif.isBlank() ? motif.trim() : null;
        if ("REJETE".equals(decision) && motifNettoye == null) {
            throw new BadRequestException("Le motif est obligatoire pour rejeter un justificatif.");
        }
        JustificatifProfessionnel justif = justificatifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justificatif introuvable"));
        List<JustificatifProfessionnel> versionsValidees = versionsRemplacees(justif);
        boolean remplacement = "VALIDE".equals(decision) && !versionsValidees.isEmpty();
        justif.setStatutValidation(decision);
        if ("VALIDE".equals(decision)) {
            // Elle fait désormais foi : ce n'est plus une mise à jour en attente.
            justif.setRemplaceId(null);
        }
        JustificatifProfessionnel saved = justificatifRepository.save(justif);
        if (remplacement) {
            // La nouvelle version fait foi : l'ancienne est retirée (fichier et ligne), ainsi que
            // les versions refusées qui la visaient.
            for (JustificatifProfessionnel ancienne : versionsValidees) {
                justificatifRepository.findByRemplaceId(ancienne.getId()).stream()
                        .filter(v -> !v.getId().equals(saved.getId()))
                        .forEach(this::supprimerFichierEtLigne);
                supprimerFichierEtLigne(ancienne);
            }
        }
        journalDecisions.enregistrer(com.psyavocat.service.support.JournalDecisions.TYPE_JUSTIFICATIF,
                saved.getId(), decision, motifNettoye);
        notificationService.sendNotification(saved.getProfessionnel().getId(),
                "VALIDE".equals(decision) ? "JUSTIFICATIF_VALIDE" : "JUSTIFICATIF_REJETE",
                "VALIDE".equals(decision)
                        ? (remplacement
                                ? "La nouvelle version de votre " + libelleType(saved.getTypeDocument()) + " (« "
                                        + saved.getNomFichier() + " ») a été validée : elle remplace la précédente."
                                : "Votre justificatif « " + saved.getNomFichier() + " » a été validé.")
                        : "Votre justificatif « " + saved.getNomFichier() + " » a été refusé. Motif : " + motifNettoye
                                + (versionsValidees.isEmpty()
                                        ? ". Vous pouvez déposer un nouveau document depuis votre profil."
                                        : ". Votre version actuelle reste en vigueur ; vous pouvez en déposer une autre depuis votre profil."),
                null);
        return mapToDto(saved);
    }

    /**
     * Versions validées qu'un document remplacera s'il est approuvé : même type pour un document
     * obligatoire, justificatif visé pour une mise à jour de justificatif complémentaire.
     */
    private List<JustificatifProfessionnel> versionsRemplacees(JustificatifProfessionnel justif) {
        if (!estDocumentUnique(justif.getTypeDocument())) {
            return justif.getRemplaceId() == null ? List.of()
                    : justificatifRepository.findById(justif.getRemplaceId())
                            .filter(v -> "VALIDE".equalsIgnoreCase(v.getStatutValidation()))
                            .map(List::of)
                            .orElse(List.of());
        }
        return justificatifRepository.findByProfessionnelIdAndTypeDocumentAndStatutValidation(
                        justif.getProfessionnel().getId(), justif.getTypeDocument(), "VALIDE").stream()
                .filter(v -> !v.getId().equals(justif.getId()))
                .toList();
    }

    private boolean estAdministrateur() {
        return authenticationContext.getCurrentAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATEUR"));
    }

    private JustificatifResponseDTO mapToDto(JustificatifProfessionnel justif) {
        String presignedUrl = null;
        if (justif.getCleObjet() != null && documentStorageService.isAvailable()) {
            try {
                presignedUrl = documentStorageService.generatePresignedDownloadUrl(justif.getCleObjet(), Duration.ofMinutes(15));
            } catch (Exception e) {
                log.debug("Impossible de générer l'URL présignée dans mapToDto: {}", e.getMessage());
            }
        }

        // Décision de l'administration : le professionnel voit pourquoi son document a été refusé.
        String motifRejet = null;
        java.time.LocalDateTime dateDecision = null;
        if ("VALIDE".equalsIgnoreCase(justif.getStatutValidation()) || "REJETE".equalsIgnoreCase(justif.getStatutValidation())) {
            var decision = journalDecisions.derniere(justif.getId());
            if (decision != null) {
                dateDecision = decision.getDateDecision();
                if ("REJETE".equalsIgnoreCase(justif.getStatutValidation())) {
                    motifRejet = decision.getMotif();
                }
            }
        }

        return JustificatifResponseDTO.builder()
                .id(justif.getId())
                .motifRejet(motifRejet)
                .dateDecision(dateDecision)
                .nomFichier(justif.getNomFichier())
                .typeDocument(justif.getTypeDocument())
                .statutValidation(justif.getStatutValidation())
                .dateDepot(justif.getDateDepot())
                .professionnelId(justif.getProfessionnel().getId())
                .tailleOctets(justif.getTailleOctets())
                .typeMime(justif.getTypeMime())
                .remplaceId(justif.getRemplaceId())
                .urlVisualisation(presignedUrl)
                .build();
    }
}
