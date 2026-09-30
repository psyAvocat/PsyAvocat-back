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
}
