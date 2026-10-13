package com.psyavocat.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.psyavocat.entity.Avocat;
import com.psyavocat.mapper.UserMapper;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.NotificationService;
import com.psyavocat.storage.model.StoredImage;
import com.psyavocat.storage.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Photo de profil : seule la clé d'objet est enregistrée, jamais l'URL présignée. */
@ExtendWith(MockitoExtension.class)
class ProfilPhotoTest {

    @Mock private AuthenticationContext authenticationContext;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AvocatRepository avocatRepository;
    @Mock private PsychologueRepository psychologueRepository;
    @Mock private SpecialiteRepository specialiteRepository;
    @Mock private UserMapper userMapper;
    @Mock private ImageStorageService imageStorageService;
    @Mock private AdministrateurRepository administrateurRepository;
    @Mock private NotificationService notificationService;
    @Mock private ClientRepository clientRepository;
    @Mock private ObjectProvider<FirebaseAuth> firebaseAuthProvider;

    @InjectMocks private ProfilServiceImpl service;

    private final MockMultipartFile image =
            new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
    private Avocat avocat;

    @BeforeEach
    void setUp() {
        avocat = new Avocat();
        avocat.setId("uid-avocat");
        avocat.setPhotoObjectKey("avatars/ancienne.jpg");
        avocat.setPhotoUrl("https://ancienne-url");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("uid-avocat");
        when(utilisateurRepository.findById("uid-avocat")).thenReturn(Optional.of(avocat));
        when(imageStorageService.uploadImage(any(), eq("avatars"), eq("uid-avocat"))).thenReturn(
                StoredImage.builder().objectKey("avatars/nouvelle.jpg")
                        .url("https://r2.example/avatars/nouvelle.jpg?X-Amz-Signature=" + "a".repeat(600)).build());
    }

    @Test
    @DisplayName("La clé est enregistrée, l'URL présignée trop longue ne l'est pas")
    void enregistreLaCle_etRetireLAncienne() {
        when(utilisateurRepository.saveAndFlush(avocat)).thenReturn(avocat);

        service.uploadPhotoProfil(image);

        assertThat(avocat.getPhotoObjectKey()).isEqualTo("avatars/nouvelle.jpg");
        assertThat(avocat.getPhotoUrl()).isNull();
        verify(imageStorageService).deleteImage("avatars/ancienne.jpg");
        verify(imageStorageService, never()).deleteImage("avatars/nouvelle.jpg");
    }

    @Test
    @DisplayName("Échec d'enregistrement : la nouvelle image est supprimée, l'ancienne conservée")
    void echecEnregistrement_nouvelleImageSupprimee() {
        when(utilisateurRepository.saveAndFlush(avocat)).thenThrow(new DataIntegrityViolationException("x"));

        assertThatThrownBy(() -> service.uploadPhotoProfil(image)).isInstanceOf(DataIntegrityViolationException.class);

        verify(imageStorageService).deleteImage("avatars/nouvelle.jpg");
        verify(imageStorageService, never()).deleteImage("avatars/ancienne.jpg");
    }
}
