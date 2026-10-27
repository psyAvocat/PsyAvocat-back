package com.psyavocat.controller;

import com.psyavocat.dto.justificatif.JustificatifResponseDTO;
import com.psyavocat.service.JustificatifService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/justificatifs")
public class JustificatifController {

    private final JustificatifService justificatifService;

    public JustificatifController(JustificatifService justificatifService) {
        this.justificatifService = justificatifService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JustificatifResponseDTO> uploadJustificatif(
            @RequestParam("file") MultipartFile file,
            @RequestParam("typeDocument") String typeDocument
    ) {
        return ResponseEntity.ok(justificatifService.uploadJustificatif(file, typeDocument));
    }

    @GetMapping("/me")
    public ResponseEntity<List<JustificatifResponseDTO>> getMyJustificatifs() {
        return ResponseEntity.ok(justificatifService.getMyJustificatifs());
    }

    /** Nouvelles versions de documents déposées par des comptes déjà validés, à vérifier. */
    @GetMapping("/mises-a-jour")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<java.util.List<com.psyavocat.dto.justificatif.JustificatifResponseDTO>> getMisesAJourEnAttente() {
        return ResponseEntity.ok(justificatifService.getMisesAJourEnAttente());
    }

    @GetMapping("/professionnel/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<JustificatifResponseDTO>> getJustificatifsByProfessionnel(@PathVariable String id) {
        return ResponseEntity.ok(justificatifService.getJustificatifsByProfessionnel(id));
    }

    /** Types affichables directement dans le navigateur ; tout autre fichier est téléchargé. */
    private static final Set<MediaType> TYPES_AFFICHABLES = Set.of(
            MediaType.APPLICATION_PDF, MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG, MediaType.parseMediaType("image/webp"));

    /**
     * Fichier d'un justificatif (stockage R2 ou disque local), pour l'auteur ou l'administration.
     * {@code inline=true} : PDF et images s'affichent dans le navigateur (consultation) ;
     * sinon, ou pour les autres formats, le fichier est téléchargé.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadJustificatif(@PathVariable String id,
                                                         @RequestParam(defaultValue = "false") boolean inline) {
        Resource resource = justificatifService.downloadJustificatif(id);
        String nom = resource.getFilename() != null ? resource.getFilename() : "document";
        MediaType type = MediaTypeFactory.getMediaType(nom).orElse(MediaType.APPLICATION_OCTET_STREAM);
        boolean affichable = inline && TYPES_AFFICHABLES.contains(type);

        ContentDisposition disposition = (affichable ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(nom, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(affichable ? type : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<java.util.Map<String, String>> getPresignedUrl(@PathVariable String id) {
        String presignedUrl = justificatifService.getPresignedUrl(id);
        if (presignedUrl == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("url", presignedUrl));
    }

    /** Décision de l'administration : {"statut": "VALIDE" | "REJETE", "motif": "..."}. */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<JustificatifResponseDTO> decider(@PathVariable String id,
                                                          @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(justificatifService.decider(id, body.get("statut"), body.get("motif")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJustificatif(@PathVariable String id) {
        justificatifService.deleteJustificatif(id);
        return ResponseEntity.noContent().build();
    }
}
