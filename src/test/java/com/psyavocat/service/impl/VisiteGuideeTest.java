package com.psyavocat.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.psyavocat.entity.Client;
import com.psyavocat.mapper.UserMapper;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.NotificationService;
import com.psyavocat.storage.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Visite guidée de l'application : proposée une seule fois par compte. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VisiteGuideeTest {

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

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId("client-1");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(utilisateurRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(utilisateurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Première fois : la date est enregistrée sur le compte")
    void premiereFois_enregistree() {
        service.marquerVisiteGuideeVue();

        assertThat(client.getVisiteGuideeVueLe()).isNotNull();
        verify(utilisateurRepository).save(client);
    }

    @Test
    @DisplayName("Déjà vue (autre appareil) : la date d'origine est conservée")
    void dejaVue_dateConservee() {
        LocalDateTime premiere = LocalDateTime.of(2026, 10, 1, 9, 0);
        client.setVisiteGuideeVueLe(premiere);

        service.marquerVisiteGuideeVue();

        assertThat(client.getVisiteGuideeVueLe()).isEqualTo(premiere);
        verify(utilisateurRepository, never()).save(any());
    }
}
