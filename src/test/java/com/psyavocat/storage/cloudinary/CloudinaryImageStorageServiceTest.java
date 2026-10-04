package com.psyavocat.storage.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
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

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageStorageServiceTest {

    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;

    private CloudinaryImageStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new CloudinaryImageStorageService(cloudinary);
    }

    @Test
    @DisplayName("Upload image valide - Succès et retour des métadonnées sécurisées")
    void testUploadImageValide() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v123/avatar.jpg",
                "public_id", "avatars/pro_123_uuid",
                "format", "jpg",
                "bytes", 1024L,
                "width", 400,
                "height", 400
        ));

        MockMultipartFile file = new MockMultipartFile(
                "file", "portrait.jpg", "image/jpeg", "image content".getBytes()
        );

        StoredImage result = storageService.uploadImage(file, "avatars", "pro_123");

        assertNotNull(result);
        assertEquals("https://res.cloudinary.com/demo/image/upload/v123/avatar.jpg", result.getUrl());
        assertEquals("avatars/pro_123_uuid", result.getPublicId());
        assertEquals("jpg", result.getFormat());
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test
    @DisplayName("Upload fichier vide - Refus avec BadRequestException")
    void testUploadFichierVide() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        assertThrows(BadRequestException.class, () -> storageService.uploadImage(file, "avatars", "pro_123"));
    }

    @Test
    @DisplayName("Upload type non autorisé (ex: executable ou script) - Refus avec BadRequestException")
    void testUploadTypeInterdit() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.sh", "application/x-sh", "echo hello".getBytes()
        );

        assertThrows(BadRequestException.class, () -> storageService.uploadImage(file, "avatars", "pro_123"));
    }

    @Test
    @DisplayName("Upload taille excessive (> 5 Mo) - Refus avec BadRequestException")
    void testUploadTailleTropGrande() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6 Mo
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", largeBytes
        );

        assertThrows(BadRequestException.class, () -> storageService.uploadImage(file, "avatars", "pro_123"));
    }

    @Test
    @DisplayName("Service Cloudinary non configuré - Exception 503 SERVICE_UNAVAILABLE")
    void testServiceIndisponible() {
        CloudinaryImageStorageService unconfiguredService = new CloudinaryImageStorageService(null);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "data".getBytes()
        );

        ApiException ex = assertThrows(ApiException.class, () -> unconfiguredService.uploadImage(file, "avatars", "pro_123"));
        assertEquals(503, ex.getStatus().value());
    }

    @Test
    @DisplayName("Suppression d'image - Invoque l'uploader Cloudinary destroy")
    void testDeleteImage() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);

        storageService.deleteImage("avatars/old_photo_id");

        verify(uploader, times(1)).destroy(eq("avatars/old_photo_id"), anyMap());
    }
}
