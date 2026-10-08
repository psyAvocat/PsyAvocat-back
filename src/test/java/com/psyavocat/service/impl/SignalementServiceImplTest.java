package com.psyavocat.service.impl;

import com.psyavocat.dto.signalement.CreateSignalementRequest;
import com.psyavocat.dto.signalement.SignalementCreeDTO;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.AdministrateurRepository;
import com.psyavocat.repository.ProfessionnelRepository;
import com.psyavocat.repository.SignalementRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.NotificationService;
import com.psyavocat.service.notification.NotificationEvenement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignalementServiceImplTest {

    @Mock private SignalementRepository signalementRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ProfessionnelRepository professionnelRepository;
    @Mock private AdministrateurRepository administrateurRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuthenticationContext authenticationContext;

    @InjectMocks private SignalementServiceImpl service;

    private Client client;
    private Avocat avocat;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId("client-1");
        avocat = new Avocat();
        avocat.setId("avocat-1");
        avocat.setNom("Bernard");
        avocat.setPrenom("Thomas");
    }

    private CreateSignalementRequest requete(MotifSignalement motif, String description) {
        return new CreateSignalementRequest("avocat-1", motif, description);
    }

    @Test
    @DisplayName("Un client signale un professionnel : enregistré EN_ATTENTE, auteur = utilisateur connecté, admins notifiés")
    void signalement_succes() {
        Administrateur admin = new Administrateur();
        admin.setId("admin-1");
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(utilisateurRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(professionnelRepository.findById("avocat-1")).thenReturn(Optional.of(avocat));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(i -> {
            Signalement s = i.getArgument(0);
            s.setId("sig-1");
            return s;
        });
        when(administrateurRepository.findAll()).thenReturn(List.of(admin));

        SignalementCreeDTO dto = service.signalerProfessionnel(requete(MotifSignalement.COMPORTEMENT_INAPPROPRIE, null));

        ArgumentCaptor<Signalement> captor = ArgumentCaptor.forClass(Signalement.class);
        verify(signalementRepository).save(captor.capture());
        assertThat(captor.getValue().getAuteur()).isSameAs(client);
        assertThat(captor.getValue().getUtilisateurVise()).isSameAs(avocat);
        assertThat(captor.getValue().getMotif()).isEqualTo("Comportement inapproprié");
        assertThat(dto.statut()).isEqualTo("EN_ATTENTE");
        verify(notificationService).notifier(any(NotificationEvenement.class));
    }

    @Test
    @DisplayName("Un professionnel ne peut pas créer de signalement client")
    void signalement_nonClient_refuse() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocat));

        assertThatThrownBy(() -> service.signalerProfessionnel(requete(MotifSignalement.AUTRE, "Description suffisante")))
                .isInstanceOf(ForbiddenException.class);
        verify(signalementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Professionnel inexistant : 404")
    void signalement_professionnelInexistant() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(utilisateurRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(professionnelRepository.findById("avocat-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.signalerProfessionnel(requete(MotifSignalement.COMPORTEMENT_INAPPROPRIE, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Motif « Autre » sans description suffisante : refusé")
    void signalement_autreSansDescription() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(utilisateurRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(professionnelRepository.findById("avocat-1")).thenReturn(Optional.of(avocat));

        assertThatThrownBy(() -> service.signalerProfessionnel(requete(MotifSignalement.AUTRE, "court")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Un seul signalement en attente par client et par professionnel")
    void signalement_doublonEnAttente() {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("client-1");
        when(utilisateurRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(professionnelRepository.findById("avocat-1")).thenReturn(Optional.of(avocat));
        when(signalementRepository.existsByAuteurIdAndUtilisateurViseIdAndStatut("client-1", "avocat-1", "EN_ATTENTE"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.signalerProfessionnel(requete(MotifSignalement.COMPORTEMENT_INAPPROPRIE, null)))
                .isInstanceOf(ConflictException.class);
    }
}
