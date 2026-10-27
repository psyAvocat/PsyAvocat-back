package com.psyavocat.service.impl;

import com.psyavocat.dto.messagerie.CreateConversationRequest;
import com.psyavocat.dto.messagerie.SendMessageRequest;
import com.psyavocat.entity.*;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.mapper.MediaUrlResolver;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.NotificationService;
import com.psyavocat.service.support.AccesProfessionnel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Un professionnel répond à toute personne qui l'a contacté, même sans relation existante. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessagerieReponseTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageContactRepository messageContactRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private AuthenticationContext authenticationContext;
    @Mock private NotificationService notificationService;
    @Mock private RealtimeGateway realtimeGateway;
    @Mock private MediaUrlResolver mediaUrlResolver;
    @Mock private AccesProfessionnel accesProfessionnel;
    @Mock private RendezVousRepository rendezVousRepository;
    @Mock private SoumissionDossierRepository soumissionDossierRepository;
    @Mock private FichePatientRepository fichePatientRepository;

    @InjectMocks private MessagerieServiceImpl service;

    private Avocat avocat;
    private Client client;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        avocat = new Avocat();
        avocat.setId("avocat-1");
        avocat.setStatutValidation("APPROVED");
        client = new Client();
        client.setId("client-1");
        conversation = new Conversation();
        conversation.setId("conv-1");
        conversation.setParticipants(new ArrayList<>(List.of(client, avocat)));

        when(authenticationContext.getRequiredFirebaseUid()).thenReturn("avocat-1");
        when(utilisateurRepository.findById("avocat-1")).thenReturn(Optional.of(avocat));
        when(utilisateurRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(messageContactRepository.save(any(MessageContact.class))).thenAnswer(i -> i.getArgument(0));
        // Aucune relation : ni rendez-vous, ni dossier reçu.
        when(rendezVousRepository.existsByProfessionnelIdAndPatientId(any(), any())).thenReturn(false);
        when(soumissionDossierRepository.existsByAvocatIdAndDossierJusticiableId(any(), any())).thenReturn(false);
    }

    @Test
    @DisplayName("Réponse à une personne qui a écrit la première : acceptée")
    void reponseAUnePriseDeContact_acceptee() {
        when(messageContactRepository.existsByConversationIdAndExpediteurId("conv-1", "client-1")).thenReturn(true);

        var dto = service.sendMessage("conv-1", new SendMessageRequest(null, "Bonjour, je peux vous aider."));

        assertThat(dto.getContenu()).isEqualTo("Bonjour, je peux vous aider.");
        verify(messageContactRepository).save(any(MessageContact.class));
    }

    @Test
    @DisplayName("Message à une personne sans relation qui n'a jamais écrit : refusé")
    void sansPriseDeContactNiRelation_refuse() {
        when(messageContactRepository.existsByConversationIdAndExpediteurId("conv-1", "client-1")).thenReturn(false);

        assertThatThrownBy(() -> service.sendMessage("conv-1", new SendMessageRequest(null, "Bonjour")))
                .isInstanceOf(ForbiddenException.class);
        verify(messageContactRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nouvelle conversation vers une personne qui n'est pas cliente : refusée")
    void nouvelleConversationSansRelation_refusee() {
        when(conversationRepository.findByParticipantId("avocat-1")).thenReturn(List.of());
        CreateConversationRequest request = new CreateConversationRequest();
        request.setDestinataireId("client-1");
        request.setPremierMessage("Bonjour");

        assertThatThrownBy(() -> service.createConversation(request)).isInstanceOf(ForbiddenException.class);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Le professionnel démarre une conversation avec l'un de ses clients : créée")
    void nouvelleConversationAvecUnClient_creee() {
        when(conversationRepository.findByParticipantId("avocat-1")).thenReturn(List.of());
        when(rendezVousRepository.existsByProfessionnelIdAndPatientId("avocat-1", "client-1")).thenReturn(true);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> {
            Conversation c = i.getArgument(0);
            c.setId("conv-nouvelle");
            return c;
        });
        CreateConversationRequest request = new CreateConversationRequest();
        request.setDestinataireId("client-1");
        request.setPremierMessage("Bonjour, je reviens vers vous.");

        var dto = service.createConversation(request);

        assertThat(dto.getId()).isEqualTo("conv-nouvelle");
        verify(messageContactRepository).save(argThat(m ->
                "Bonjour, je reviens vers vous.".equals(m.getContenu()) && m.getExpediteur() == avocat));
        // Le client est prévenu (notification + push).
        verify(notificationService).notifier(argThat(e -> "client-1".equals(e.destinataireId())));
    }
}
