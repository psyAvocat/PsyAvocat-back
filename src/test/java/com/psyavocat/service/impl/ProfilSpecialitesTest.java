package com.psyavocat.service.impl;

import com.psyavocat.dto.profil.CreateAvocatRequest;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Specialite;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.mapper.UserMapper;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticatedUser;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.NotificationService;
import com.psyavocat.storage.service.ImageStorageService;
import com.google.firebase.auth.FirebaseAuth;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Une spécialité au moins est exigée à l'inscription d'un professionnel. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfilSpecialitesTest {

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

    @BeforeEach
    void connecte() {
        when(authenticationContext.getCurrentUser()).thenReturn(Optional.of(
                new AuthenticatedUser("uid-avocat", "avocat@test.fr", true, null, List.of())));
        when(utilisateurRepository.findById("uid-avocat")).thenReturn(Optional.empty());
        when(utilisateurRepository.findAllTelephones()).thenReturn(List.of());
    }

    private CreateAvocatRequest requete(List<String> specialites) {
        CreateAvocatRequest request = new CreateAvocatRequest();
        request.setNom("Diallo");
        request.setPrenom("Awa");
        request.setNumeroBarreau("B-123");
        request.setSpecialiteIds(specialites);
        return request;
    }

    private Specialite specialite(String id, String type) {
        Specialite s = new Specialite();
        s.setId(id);
        s.setNom(id);
        s.setTypeProfessionnel(type);
        return s;
    }

    @Test
    @DisplayName("Inscription d'un avocat sans spécialité refusée")
    void sansSpecialite_refusee() {
        assertThatThrownBy(() -> service.createAvocatProfile(requete(List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("au moins une spécialité");
        verify(avocatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Spécialité inconnue refusée")
    void specialiteInconnue_refusee() {
        when(specialiteRepository.findAllById(List.of("spe-x"))).thenReturn(List.of());

        assertThatThrownBy(() -> service.createAvocatProfile(requete(List.of("spe-x"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    @DisplayName("Spécialité psychologique refusée pour un avocat")
    void specialiteAutreUnivers_refusee() {
        when(specialiteRepository.findAllById(List.of("spe-anxiete")))
                .thenReturn(List.of(specialite("spe-anxiete", "PSYCHOLOGUE")));

        assertThatThrownBy(() -> service.createAvocatProfile(requete(List.of("spe-anxiete"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("pas applicable");
    }

    @Test
    @DisplayName("Avocat inscrit avec ses spécialités")
    void avecSpecialite_enregistre() {
        Specialite famille = specialite("spe-famille", "AVOCAT");
        when(specialiteRepository.findAllById(List.of("spe-famille"))).thenReturn(List.of(famille));
        when(avocatRepository.save(any(Avocat.class))).thenAnswer(i -> i.getArgument(0));
        when(administrateurRepository.findAll()).thenReturn(List.of());

        service.createAvocatProfile(requete(List.of("spe-famille")));

        verify(avocatRepository).save(argThat(a -> a.getSpecialites().contains(famille)));
        assertThat(famille.getTypeProfessionnel()).isEqualTo("AVOCAT");
    }
}
