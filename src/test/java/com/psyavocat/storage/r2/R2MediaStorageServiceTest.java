package com.psyavocat.storage.r2;

import com.psyavocat.exception.ApiException;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.storage.model.StoredImage;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class R2MediaStorageServiceTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;

    private R2MediaStorageService r2MediaService;

    @BeforeEach
    void setUp() {
        r2MediaService = new R2MediaStorageService(s3Client, s3Presigner);
        ReflectionTestUtils.setField(r2MediaService, "mediaBucket", "psyavocat-media");
    }

    @Test
    @DisplayName("Upload image valide - Téléversement réussi dans le bucket R2 média (psyavocat-media)")
    void testUploadImageValide() throws MalformedURLException {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://account.r2.cloudflarestorage.com/psyavocat-media/avatars/pro1_uuid.jpg"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "image-bytes".getBytes()
        );

        StoredImage result = r2MediaService.uploadImage(file, "avatars", "pro1");

        assertNotNull(result);
        assertNotNull(result.getObjectKey());
        assertTrue(result.getObjectKey().startsWith("avatars/pro1_"));
        assertTrue(result.getObjectKey().endsWith(".jpg"));
        assertEquals("jpg", result.getFormat());
        assertEquals("image-bytes".length(), result.getBytes());
        assertNotNull(result.getUrl());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Upload type non autorisé (ex: PDF ou GIF) - Rejeté avec BadRequestException")
    void testUploadTypeInterdit() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "data".getBytes()
        );

        assertThrows(BadRequestException.class, () -> r2MediaService.uploadImage(file, "avatars", "pro1"));
    }

    @Test
    @DisplayName("Upload taille excessive (> 5 Mo) - Rejeté avec BadRequestException")
    void testUploadTailleTropGrande() {
        byte[] bigFile = new byte[6 * 1024 * 1024]; // 6 Mo
        MockMultipartFile file = new MockMultipartFile(
                "file", "gros.png", "image/png", bigFile
        );

        assertThrows(BadRequestException.class, () -> r2MediaService.uploadImage(file, "avatars", "pro1"));
    }

    @Test
    @DisplayName("Suppression image R2 - Invoque deleteObject sur S3Client avec le bucket média")
    void testDeleteImage() {
        r2MediaService.deleteImage("avatars/pro1_uuid.jpg");

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("R2 média non configuré - Exception 503 SERVICE_UNAVAILABLE")
    void testR2MediaIndisponible() {
        R2MediaStorageService unconfigured = new R2MediaStorageService(null, null);
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "data".getBytes()
        );

        ApiException ex = assertThrows(ApiException.class, () -> unconfigured.uploadImage(file, "avatars", "pro1"));
        assertEquals(503, ex.getStatus().value());
    }
}
