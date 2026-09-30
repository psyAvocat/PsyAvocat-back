package com.psyavocat.service.impl;

import com.psyavocat.dto.justificatif.JustificatifResponseDTO;
import com.psyavocat.entity.JustificatifProfessionnel;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.JustificatifProfessionnelRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.JustificatifService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class JustificatifServiceImpl implements JustificatifService {

    private final JustificatifProfessionnelRepository justificatifRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;
    private final Path fileStorageLocation;

    public JustificatifServiceImpl(
            JustificatifProfessionnelRepository justificatifRepository,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext
    ) {
        this.justificatifRepository = justificatifRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
        this.fileStorageLocation = Paths.get("uploads/justificatifs").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
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

        if (file.isEmpty()) {
            throw new BadRequestException("Fichier vide.");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new BadRequestException("Sorry! Filename contains invalid path sequence " + originalFileName);
        }
        
        String fileExtension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            fileExtension = originalFileName.substring(i);
        }
        
        String newFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(newFileName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }

        JustificatifProfessionnel justif = new JustificatifProfessionnel();
        justif.setNomFichier(originalFileName);
        justif.setTypeDocument(typeDocument);
        justif.setCheminStockage(targetLocation.toString());
        justif.setStatutValidation("EN_ATTENTE");
        justif.setDateDepot(LocalDate.now());
        justif.setProfessionnel(professionnel);

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

        String uid = authenticationContext.getRequiredFirebaseUid();
        boolean isAdmin = authenticationContext.getCurrentUser().get().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATEUR"));

        if (!isAdmin && !justif.getProfessionnel().getId().equals(uid)) {
            throw new BadRequestException("Vous n'êtes pas autorisé à accéder à ce document.");
        }

        try {
            Path filePath = Paths.get(justif.getCheminStockage()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Fichier introuvable sur le disque");
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Fichier introuvable sur le disque: " + ex.getMessage());
        }
    }

    private JustificatifResponseDTO mapToDto(JustificatifProfessionnel justif) {
        return JustificatifResponseDTO.builder()
                .id(justif.getId())
                .nomFichier(justif.getNomFichier())
                .typeDocument(justif.getTypeDocument())
                .statutValidation(justif.getStatutValidation())
                .dateDepot(justif.getDateDepot())
                .professionnelId(justif.getProfessionnel().getId())
                .build();
    }
}
