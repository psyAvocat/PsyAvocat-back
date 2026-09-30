package com.psyavocat.controller;

import com.psyavocat.dto.justificatif.JustificatifResponseDTO;
import com.psyavocat.service.JustificatifService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping("/professionnel/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<JustificatifResponseDTO>> getJustificatifsByProfessionnel(@PathVariable String id) {
        return ResponseEntity.ok(justificatifService.getJustificatifsByProfessionnel(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadJustificatif(@PathVariable String id) {
        Resource resource = justificatifService.downloadJustificatif(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
