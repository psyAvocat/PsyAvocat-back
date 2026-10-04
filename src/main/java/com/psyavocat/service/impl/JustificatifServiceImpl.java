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
    private final Path fileStorageLocation;

    public JustificatifServiceImpl(
            JustificatifProfessionnelRepository justificatifRepository,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext,
            DocumentStorageService documentStorageService
    ) {
        this.justificatifRepository = justificatifRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
        this.documentStorageService = documentStorageService;
        this.fileStorageLocation = Paths.get("uploads/justificatifs").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.warn("Impossible d'initialiser le répertoire local de justificatifs : {}", ex.getMessage());
        }
    }

    @Override
    public JustificatifResponseDTO uploadJustificatif(MultipartFile file, String typeDocument) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (!(utilisateur instanceof Professionnel professionnel)) {
            throw new BadRequestException("Seul un professionnel peut uploader un justificatif.");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier manquant ou vide.");
        }

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

        JustificatifProfessionnel saved = justificatifRepository.save(justif);
        return mapToDto(saved);
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
        boolean isAdmin = authenticationContext.getCurrentAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATEUR"));

        if (!isAdmin && !justif.getProfessionnel().getId().equals(uid)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à accéder à ce document.");
        }
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

        return JustificatifResponseDTO.builder()
                .id(justif.getId())
                .nomFichier(justif.getNomFichier())
                .typeDocument(justif.getTypeDocument())
                .statutValidation(justif.getStatutValidation())
                .dateDepot(justif.getDateDepot())
                .professionnelId(justif.getProfessionnel().getId())
                .tailleOctets(justif.getTailleOctets())
                .typeMime(justif.getTypeMime())
                .urlVisualisation(presignedUrl)
                .build();
    }
}
