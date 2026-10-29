package com.psyavocat.service;

import com.psyavocat.dto.messagerie.*;

import java.util.List;

/**
 * Contrat de service pour la messagerie et les conversations entre usagers et praticiens.
 */
public interface MessagerieService {

    List<ConversationResponseDTO> getMesConversations();

    List<MessageResponseDTO> getMessages(String conversationId);

    /**
     * Discussion à ouvrir avec ce destinataire (bouton « Écrire ») : celle qui existe déjà si elle
     * est visible pour l'appelant, sinon un brouillon sans identifiant. Rien n'est enregistré :
     * la discussion n'est créée qu'avec le premier message ({@link #createConversation}).
     */
    ConversationResponseDTO ouvrirAvec(String destinataireId);

    ConversationResponseDTO createConversation(CreateConversationRequest request);

    MessageResponseDTO sendMessage(String conversationId, SendMessageRequest request);

    long getNombreMessagesNonLus();

    /** L'expéditeur supprime l'un de ses messages : il disparaît pour tous les participants. */
    void supprimerMessage(String conversationId, String messageId);

    /** Supprime la discussion de la messagerie de l'appelant (l'autre participant la conserve). */
    void supprimerConversation(String conversationId);
}
