package com.psyavocat.storage.r2;

import com.psyavocat.exception.ApiException;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.storage.model.StoredImage;
import com.psyavocat.storage.service.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service de stockage Cloudflare R2 pour les médias visuels (photos de profil, avatars).
 * Utilise le bucket dédié `R2_MEDIA_BUCKET` (psyavocat-media).
 */
@Service
public class R2MediaStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(R2MediaStorageService.class);

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 Mo
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${r2.media-bucket:${R2_MEDIA_BUCKET:psyavocat-media}}")
    private String mediaBucket;

    public R2MediaStorageService(
            @Autowired(required = false) S3Client s3Client,
            @Autowired(required = false) S3Presigner s3Presigner
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public boolean isAvailable() {
        return s3Client != null && mediaBucket != null && !mediaBucket.isBlank();
    }

    @Override
    public StoredImage uploadImage(MultipartFile file, String dossier, String idPrefix) {
        validerImage(file);

        if (!isAvailable()) {
            log.error("Tentative d'upload média vers Cloudflare R2 alors que le service n'est pas configuré");
            throw new ApiException("Le service de stockage de médias Cloudflare R2 est temporairement indisponible.", HttpStatus.SERVICE_UNAVAILABLE) {};
        }

        String extension = extraireExtension(file);
        String cleanDossier = (dossier != null && !dossier.isBlank())
                ? dossier.replaceAll("^/+", "").replaceAll("/+$", "")
                : "media";
        String prefixPart = (idPrefix != null && !idPrefix.isBlank())
                ? idPrefix.trim().replaceAll("[^a-zA-Z0-9_-]", "_") + "_"
                : "";
        String objectKey = cleanDossier + "/" + prefixPart + UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(mediaBucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Image téléversée avec succès dans Cloudflare R2 (bucket={}) : cle={}", mediaBucket, objectKey);

            String url = genererUrlAcces(objectKey);

            return StoredImage.builder()
                    .url(url)
                    .publicId(objectKey)
                    .objectKey(objectKey)
                    .format(extension)
                    .bytes(file.getSize())
                    .build();

        } catch (IOException ex) {
            log.error("Erreur de lecture du flux image pour R2 : {}", ex.getMessage(), ex);
            throw new ApiException("Erreur lors de la lecture du fichier image : " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR) {};
        } catch (S3Exception ex) {
            log.error("Erreur S3/R2 lors du téléversement de l'image (cle={}) : {}", objectKey, ex.getMessage(), ex);
            throw new ApiException("Échec du téléversement vers Cloudflare R2 : " + ex.awsErrorDetails().errorMessage(), HttpStatus.BAD_GATEWAY) {};
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (!isAvailable() || publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(mediaBucket)
                    .key(publicId)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Image supprimée de Cloudflare R2 (bucket={}) : cle={}", mediaBucket, publicId);
        } catch (Exception ex) {
            log.warn("Impossible de supprimer l'image dans R2 (cle={}): {}", publicId, ex.getMessage());
        }
    }

    /** Durée de validité des URL d'affichage générées à la lecture. */
    private static final Duration DUREE_URL_AFFICHAGE = Duration.ofHours(1);

    @Override
    public String getAccessUrl(String objectKey) {
        if (s3Presigner == null || objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(DUREE_URL_AFFICHAGE)
                    .getObjectRequest(GetObjectRequest.builder().bucket(mediaBucket).key(objectKey).build())
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.warn("Impossible de générer l'URL d'accès de l'image {} : {}", objectKey, e.getMessage());
            return null;
        }
    }

    private String genererUrlAcces(String objectKey) {
        if (s3Presigner != null) {
            try {
                // Les deux buckets restant privés, on génère une URL présignée longue durée (7 jours)
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(mediaBucket)
                        .key(objectKey)
                        .build();

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofDays(7))
                        .getObjectRequest(getRequest)
                        .build();

                return s3Presigner.presignGetObject(presignRequest).url().toString();
            } catch (Exception e) {
                log.warn("Impossible de pré-générer l'URL présignée de l'image {} : {}", objectKey, e.getMessage());
            }
        }
        return "/" + mediaBucket + "/" + objectKey;
    }

    private void validerImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier image est vide ou manquant.");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("La taille de l'image (" + (file.getSize() / 1024 / 1024) + " Mo) dépasse la limite autorisée de 5 Mo.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Type d'image non autorisé : " + contentType + ". Formats acceptés : JPEG, PNG, WEBP.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new BadRequestException("Extension d'image non autorisée : ." + ext);
            }
        }
    }

    private String extraireExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        String contentType = file.getContentType();
        if ("image/png".equalsIgnoreCase(contentType)) return "png";
        if ("image/webp".equalsIgnoreCase(contentType)) return "webp";
        return "jpg";
    }
}
