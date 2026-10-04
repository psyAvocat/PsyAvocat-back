package com.psyavocat.storage.r2;

import com.psyavocat.exception.ApiException;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.storage.model.StoredDocument;
import com.psyavocat.storage.service.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Service
public class R2DocumentStorageService implements DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(R2DocumentStorageService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 Mo
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${r2.bucket-name:${R2_BUCKET_NAME:}}")
    private String bucketName;

    public R2DocumentStorageService(
            @Autowired(required = false) S3Client s3Client,
            @Autowired(required = false) S3Presigner s3Presigner
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public boolean isAvailable() {
        return s3Client != null && bucketName != null && !bucketName.isBlank();
    }

    @Override
    public StoredDocument uploadDocument(MultipartFile file, String cleObjet) {
        validerDocument(file);

        if (!isAvailable()) {
            log.error("Tentative d'upload vers Cloudflare R2 alors que le service n'est pas configuré");
            throw new ApiException("Le service de stockage Cloudflare R2 est temporairement indisponible.", HttpStatus.SERVICE_UNAVAILABLE) {};
        }

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleObjet)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Document téléversé avec succès dans Cloudflare R2 : cle={}", cleObjet);

            String presignedUrl = null;
            if (s3Presigner != null) {
                try {
                    presignedUrl = generatePresignedDownloadUrl(cleObjet, Duration.ofMinutes(15));
                } catch (Exception e) {
                    log.warn("Impossible de pré-générer l'URL présignée pour {} : {}", cleObjet, e.getMessage());
                }
            }

            return StoredDocument.builder()
                    .cleObjet(cleObjet)
                    .nomOriginal(file.getOriginalFilename())
                    .typeMime(file.getContentType())
                    .tailleOctets(file.getSize())
                    .urlPresignee(presignedUrl)
                    .build();

        } catch (IOException ex) {
            log.error("Erreur de lecture du flux du document pour R2 : {}", ex.getMessage(), ex);
            throw new ApiException("Erreur lors de la lecture du document : " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR) {};
        } catch (S3Exception ex) {
            log.error("Erreur S3/R2 lors du téléversement du document (cle={}) : {}", cleObjet, ex.getMessage(), ex);
            throw new ApiException("Échec du téléversement vers Cloudflare R2 : " + ex.awsErrorDetails().errorMessage(), HttpStatus.BAD_GATEWAY) {};
        }
    }

    @Override
    public Resource downloadDocument(String cleObjet, String nomFichierOriginal) {
        if (!isAvailable()) {
            throw new ApiException("Le service de stockage Cloudflare R2 est temporairement indisponible.", HttpStatus.SERVICE_UNAVAILABLE) {};
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleObjet)
                    .build();

            ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(getRequest);

            return new InputStreamResource(responseStream) {
                @Override
                public String getFilename() {
                    return nomFichierOriginal != null ? nomFichierOriginal : "document";
                }

                @Override
                public long contentLength() {
                    Long len = responseStream.response().contentLength();
                    return len != null ? len : -1L;
                }
            };

        } catch (NoSuchKeyException ex) {
            throw new ResourceNotFoundException("Document introuvable dans Cloudflare R2 pour la clé : " + cleObjet);
        } catch (S3Exception ex) {
            log.error("Erreur lors de la récupération du document R2 ({}) : {}", cleObjet, ex.getMessage(), ex);
            throw new ApiException("Erreur de récupération du document : " + ex.awsErrorDetails().errorMessage(), HttpStatus.BAD_GATEWAY) {};
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String cleObjet, Duration duree) {
        if (s3Presigner == null || bucketName == null || bucketName.isBlank()) {
            throw new ApiException("Le générateur d'URL présignées R2 n'est pas configuré.", HttpStatus.SERVICE_UNAVAILABLE) {};
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleObjet)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duree != null ? duree : Duration.ofMinutes(15))
                    .getObjectRequest(getRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();

        } catch (Exception ex) {
            log.error("Erreur lors de la génération de l'URL présignée pour {} : {}", cleObjet, ex.getMessage(), ex);
            throw new ApiException("Impossible de générer l'accès temporaire au document.", HttpStatus.INTERNAL_SERVER_ERROR) {};
        }
    }

    @Override
    public void deleteDocument(String cleObjet) {
        if (!isAvailable() || cleObjet == null || cleObjet.isBlank()) {
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleObjet)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Document supprimé avec succès de Cloudflare R2 : cle={}", cleObjet);
        } catch (Exception ex) {
            log.warn("Impossible de supprimer le document dans R2 (cle={}): {}", cleObjet, ex.getMessage());
        }
    }

    private void validerDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier document est vide ou manquant.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("La taille du document (" + (file.getSize() / 1024 / 1024) + " Mo) dépasse la limite autorisée de 10 Mo.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Type de document non autorisé : " + contentType + ". Formats acceptés : PDF, JPEG, PNG, DOC, DOCX.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new BadRequestException("Extension de document non autorisée : ." + ext);
            }
        }
    }
}
