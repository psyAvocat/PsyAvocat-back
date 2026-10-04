package com.psyavocat.storage.r2;

import com.psyavocat.exception.ApiException;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.storage.model.StoredDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class R2DocumentStorageServiceTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;

    private R2DocumentStorageService r2Service;

    @BeforeEach
    void setUp() {
        r2Service = new R2DocumentStorageService(s3Client, s3Presigner);
        ReflectionTestUtils.setField(r2Service, "bucketName", "psyavocat-justificatifs-prive");
    }

    @Test
    @DisplayName("Upload document valide - Succès et téléversement dans le bucket R2 privé")
    void testUploadDocumentValide() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "diplome.pdf", "application/pdf", "contenu pdf diplome".getBytes()
        );

        StoredDocument result = r2Service.uploadDocument(file, "justificatifs/pro_1/uuid-123.pdf");

        assertNotNull(result);
        assertEquals("justificatifs/pro_1/uuid-123.pdf", result.getCleObjet());
        assertEquals("diplome.pdf", result.getNomOriginal());
        assertEquals("application/pdf", result.getTypeMime());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Upload type non autorisé (ex: executable exe) - Refus avec BadRequestException")
    void testUploadTypeInterdit() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "binary".getBytes()
        );

        assertThrows(BadRequestException.class, () -> r2Service.uploadDocument(file, "justificatifs/pro_1/malware.exe"));
    }

    @Test
    @DisplayName("Upload taille excessive (> 10 Mo) - Refus avec BadRequestException")
    void testUploadTailleTropGrande() {
        byte[] bigFile = new byte[11 * 1024 * 1024]; // 11 Mo
        MockMultipartFile file = new MockMultipartFile(
                "file", "gros.pdf", "application/pdf", bigFile
        );

        assertThrows(BadRequestException.class, () -> r2Service.uploadDocument(file, "justificatifs/pro_1/gros.pdf"));
    }

    @Test
    @DisplayName("Génération URL présignée R2 - Durée de vie limitée et signature S3")
    void testGenererUrlPresignee() throws MalformedURLException {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://account.r2.cloudflarestorage.com/bucket/doc.pdf?X-Amz-Signature=xyz"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = r2Service.generatePresignedDownloadUrl("justificatifs/pro_1/uuid.pdf", Duration.ofMinutes(15));

        assertNotNull(url);
        assertTrue(url.contains("X-Amz-Signature"));
    }

    @Test
    @DisplayName("Suppression document R2 - Invoque deleteObject sur S3Client")
    void testDeleteDocument() {
        r2Service.deleteDocument("justificatifs/pro_1/uuid.pdf");

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("R2 non configuré - Exception 503 SERVICE_UNAVAILABLE")
    void testR2Indisponible() {
        R2DocumentStorageService unconfigured = new R2DocumentStorageService(null, null);
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "data".getBytes()
        );

        ApiException ex = assertThrows(ApiException.class, () -> unconfigured.uploadDocument(file, "cle.pdf"));
        assertEquals(503, ex.getStatus().value());
    }
}
