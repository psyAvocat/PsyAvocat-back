package com.psyavocat.service.impl;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Suppression d'un message (par son auteur, pour tous) et d'une discussion (pour soi). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessagerieSuppressionTest {

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
    @Mock private ConversationMasqueeRepository conversationMasqueeRepository;

    @InjectMocks private MessagerieServiceImpl service;

    private Avocat avocat;
    private Client client;
    private Conversation conversation;
    private MessageContact messageAvocat;
    private MessageContact messageClient;
    private final List<ConversationMasquee> masquages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        avocat = new Avocat();
        avocat.setId("avocat-1");
        client = new Client();
        client.setId("client-1");
        conversation = new Conversation();
        conversation.setId("conv-1");
        conversation.setParticipants(new ArrayList<>(List.of(client, avocat)));
        messageClient = message("m-client", client, LocalDateTime.now().minusHours(2));
        messageAvocat = message("m-avocat", avocat, LocalDateTime.now().minusHours(1));
        conversation.setMessages(new ArrayList<>(List.of(messageClient, messageAvocat)));

        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(conversationRepository.findByParticipantId(any())).thenReturn(List.of(conversation));
        when(messageContactRepository.findById("m-avocat")).thenReturn(Optional.of(messageAvocat));
        when(messageContactRepository.findById("m-client")).thenReturn(Optional.of(messageClient));
        when(messageContactRepository.findByConversationIdOrderByDateEnvoiAsc("conv-1"))
                .thenAnswer(i -> new ArrayList<>(conversation.getMessages()));
        when(conversationMasqueeRepository.save(any())).thenAnswer(i -> {
            ConversationMasquee m = i.getArgument(0);
            if (!masquages.contains(m)) masquages.add(m);
            return m;
        });
        when(conversationMasqueeRepository.findByConversationId("conv-1")).thenAnswer(i -> List.copyOf(masquages));
        when(conversationMasqueeRepository.findByUtilisateurId(any())).thenAnswer(i -> masquages.stream()
                .filter(m -> m.getUtilisateur().getId().equals(i.getArgument(0))).toList());
        when(conversationMasqueeRepository.findByConversationIdAndUtilisateurId(eq("conv-1"), any()))
                .thenAnswer(i -> masquages.stream()
                        .filter(m -> m.getUtilisateur().getId().equals(i.getArgument(1))).findFirst());
    }

    private MessageContact message(String id, Utilisateur expediteur, LocalDateTime date) {
        MessageContact m = new MessageContact();
        m.setId(id);
        m.setConversation(conversation);
        m.setExpediteur(expediteur);
        m.setDateEnvoi(date);
        m.setContenu("Bonjour");
        m.setLu(false);
        return m;
    }

    private void connecte(String uid) {
        when(authenticationContext.getRequiredFirebaseUid()).thenReturn(uid);
    }

    @Test
    @DisplayName("L'expéditeur supprime son message : effacé pour tous, notification non lue retirée")
    void supprimerSonMessage() {
        connecte("avocat-1");

        service.supprimerMessage("conv-1", "m-avocat");

        verify(messageContactRepository).delete(messageAvocat);
        assertThat(conversation.getMessages()).containsExactly(messageClient);
        verify(notificationService).supprimerNonLuesPourRessource("client-1", "CONVERSATION", "conv-1");
        verify(realtimeGateway).envoyerA(eq("client-1"), eq("MESSAGE"), any());
        verify(realtimeGateway).envoyerA(eq("avocat-1"), eq("MESSAGE"), any());
    }

    @Test
    @DisplayName("Impossible de supprimer le message d'un autre participant")
    void supprimerMessageDUnAutre_refuse() {
        connecte("avocat-1");

        assertThatThrownBy(() -> service.supprimerMessage("conv-1", "m-client"))
                .isInstanceOf(ForbiddenException.class);
        verify(messageContactRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Discussion supprimée par l'avocat : disparaît pour lui seul, le client la conserve")
    void supprimerDiscussionPourSoi() {
        connecte("avocat-1");
        service.supprimerConversation("conv-1");

        assertThat(service.getMesConversations()).isEmpty();
        verify(conversationRepository, never()).delete(any());

        connecte("client-1");
        assertThat(service.getMesConversations()).hasSize(1);
        assertThat(service.getMessages("conv-1")).hasSize(2);
    }

    @Test
    @DisplayName("Nouveau message après suppression : la discussion revient avec les seuls nouveaux messages")
    void discussionReapparaitAvecNouveauxMessages() {
        connecte("avocat-1");
        service.supprimerConversation("conv-1");
        MessageContact nouveau = message("m-nouveau", client, LocalDateTime.now().plusSeconds(5));
        conversation.getMessages().add(nouveau);

        assertThat(service.getMesConversations()).hasSize(1);
        assertThat(service.getMessages("conv-1")).extracting("id").containsExactly("m-nouveau");
    }

    @Test
    @DisplayName("Supprimée par les deux participants : effacée de la base")
    void supprimeeParTous_effacee() {
        connecte("avocat-1");
        service.supprimerConversation("conv-1");
        connecte("client-1");
        service.supprimerConversation("conv-1");

        verify(conversationRepository).delete(conversation);
        verify(notificationService).supprimerPourRessource("CONVERSATION", "conv-1");
    }

    @Test
    @DisplayName("Dernier message supprimé : la discussion vide est effacée pour tous")
    void dernierMessageSupprime_discussionEffacee() {
        conversation.getMessages().remove(messageClient);
        connecte("avocat-1");

        service.supprimerMessage("conv-1", "m-avocat");

        verify(conversationRepository).delete(conversation);
        verify(notificationService).supprimerPourRessource("CONVERSATION", "conv-1");
        verify(realtimeGateway).envoyerA(eq("client-1"), eq("MESSAGE"),
                argThat(e -> e instanceof java.util.Map<?, ?> map && Boolean.TRUE.equals(map.get("conversationSupprimee"))));
    }

    @Test
    @DisplayName("Il reste des messages : la discussion est conservée")
    void messagesRestants_discussionConservee() {
        connecte("avocat-1");

        service.supprimerMessage("conv-1", "m-avocat");

        verify(conversationRepository, never()).delete(any());
    }
}
