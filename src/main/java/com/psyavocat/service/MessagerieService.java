package com.psyavocat.service;

import com.psyavocat.dto.messagerie.*;

import java.util.List;

/**
 * Contrat de service pour la messagerie et les conversations entre usagers et praticiens.
 */
public interface MessagerieService {

    List<ConversationResponseDTO> getMesConversations();

    List<MessageResponseDTO> getMessages(String conversationId);

    ConversationResponseDTO createConversation(CreateConversationRequest request);

    MessageResponseDTO sendMessage(String conversationId, SendMessageRequest request);

    long getNombreMessagesNonLus();

    /**
     * Garantit l'existence d'une conversation ouverte entre deux utilisateurs
     * (notamment lors de la prise d'un rendez-vous entre un client et un professionnel).
     */
    void assurerConversationEntre(com.psyavocat.entity.Utilisateur u1, com.psyavocat.entity.Utilisateur u2);

    /**
     * Envoie automatiquement dans la conversation entre le client et le professionnel
     * le lien Google Meet ainsi que la date et l'heure du rendez-vous en visioconférence.
     */
    void envoyerMessageVisioRendezVous(com.psyavocat.entity.RendezVous rdv, boolean estModification);
}
