package com.psyavocat.storage.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.psyavocat.exception.ApiException;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.storage.model.StoredImage;
import com.psyavocat.storage.service.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class CloudinaryImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryImageStorageService.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 Mo
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Cloudinary cloudinary;

    public CloudinaryImageStorageService(@Autowired(required = false) Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public boolean isAvailable() {
        return cloudinary != null;
    }

    @Override
    public StoredImage uploadImage(MultipartFile file, String dossier, String idPrefix) {
        validerFichier(file);

        if (cloudinary == null) {
            log.error("Tentative d'upload d'image alors que Cloudinary n'est pas configuré");
            throw new ApiException("Le service de stockage Cloudinary est temporairement indisponible.", HttpStatus.SERVICE_UNAVAILABLE) {};
        }

        String safeId = (dossier != null && !dossier.isBlank() ? dossier + "/" : "") +
                (idPrefix != null && !idPrefix.isBlank() ? idPrefix + "_" : "") +
                UUID.randomUUID();

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", safeId,
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String format = (String) uploadResult.get("format");
            Number bytes = (Number) uploadResult.get("bytes");
            Number width = (Number) uploadResult.get("width");
            Number height = (Number) uploadResult.get("height");

            log.info("Image uploadée avec succès sur Cloudinary : publicId={}", publicId);

            return StoredImage.builder()
                    .url(url)
                    .publicId(publicId)
                    .format(format)
                    .bytes(bytes != null ? bytes.longValue() : file.getSize())
                    .width(width != null ? width.intValue() : null)
                    .height(height != null ? height.intValue() : null)
                    .build();

        } catch (IOException ex) {
            log.error("Erreur lors de l'upload vers Cloudinary : {}", ex.getMessage(), ex);
            throw new ApiException("Échec du téléversement de l'image vers Cloudinary : " + ex.getMessage(), HttpStatus.BAD_GATEWAY) {};
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (cloudinary == null || publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image supprimée de Cloudinary : publicId={}", publicId);
        } catch (Exception ex) {
            log.warn("Impossible de supprimer l'image Cloudinary (publicId={}): {}", publicId, ex.getMessage());
        }
    }

    private void validerFichier(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier image est vide ou manquant.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("La taille de l'image (" + (file.getSize() / 1024 / 1024) + " Mo) dépasse la limite autorisée de 5 Mo.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Type de fichier non autorisé : " + contentType + ". Formats acceptés : JPEG, PNG, WebP.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new BadRequestException("Extension de fichier non autorisée : ." + ext);
            }
        }
    }
}
