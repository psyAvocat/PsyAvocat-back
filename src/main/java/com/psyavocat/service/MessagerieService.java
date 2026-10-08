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
}
