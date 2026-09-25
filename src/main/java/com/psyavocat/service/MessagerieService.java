package com.psyavocat.service;

import com.psyavocat.dto.messagerie.*;
import com.psyavocat.entity.Conversation;
import com.psyavocat.entity.MessageContact;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.ConversationRepository;
import com.psyavocat.repository.MessageContactRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class MessagerieService {

    private final ConversationRepository conversationRepository;
    private final MessageContactRepository messageContactRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;

    public MessagerieService(
            ConversationRepository conversationRepository,
            MessageContactRepository messageContactRepository,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext
    ) {
        this.conversationRepository = conversationRepository;
        this.messageContactRepository = messageContactRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getMesConversations() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return conversationRepository.findByParticipantId(uid).stream()
                .map(c -> toConversationDto(c, uid))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessages(String conversationId) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));

        boolean isParticipant = conv.getParticipants().stream().anyMatch(p -> p.getId().equals(uid));
        if (!isParticipant) {
            throw new ForbiddenException("Vous ne faites pas partie de cette conversation");
        }

        return messageContactRepository.findByConversationIdOrderByDateEnvoiAsc(conversationId).stream()
                .map(this::toMessageDto)
                .toList();
    }

    public ConversationResponseDTO createConversation(CreateConversationRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur expediteur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Utilisateur destinataire = utilisateurRepository.findById(request.getDestinataireId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire introuvable"));

        Conversation conv = new Conversation();
        conv.setDateCreation(LocalDateTime.now());
        conv.setStatut("OUVERTE");
        conv.setParticipants(new ArrayList<>(List.of(expediteur, destinataire)));
        conv.setMessages(new ArrayList<>());

        Conversation savedConv = conversationRepository.save(conv);

        MessageContact message = new MessageContact();
        message.setConversation(savedConv);
        message.setExpediteur(expediteur);
        message.setDateEnvoi(LocalDateTime.now());
        message.setObjet(request.getObjet());
        message.setContenu(request.getPremierMessage());

        MessageContact savedMsg = messageContactRepository.save(message);
        savedConv.getMessages().add(savedMsg);

        return toConversationDto(savedConv, uid);
    }

    public MessageResponseDTO sendMessage(String conversationId, SendMessageRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));

        boolean isParticipant = conv.getParticipants().stream().anyMatch(p -> p.getId().equals(uid));
        if (!isParticipant) {
            throw new ForbiddenException("Vous ne faites pas partie de cette conversation");
        }

        Utilisateur expediteur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        MessageContact message = new MessageContact();
        message.setConversation(conv);
        message.setExpediteur(expediteur);
        message.setDateEnvoi(LocalDateTime.now());
        message.setObjet(request.getObjet());
        message.setContenu(request.getContenu());

        MessageContact saved = messageContactRepository.save(message);
        return toMessageDto(saved);
    }

    private ConversationResponseDTO toConversationDto(Conversation c, String currentUserId) {
        Utilisateur correspondant = c.getParticipants().stream()
                .filter(p -> !p.getId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        MessageResponseDTO dernierMsg = null;
        if (c.getMessages() != null && !c.getMessages().isEmpty()) {
            MessageContact last = c.getMessages().get(c.getMessages().size() - 1);
            dernierMsg = toMessageDto(last);
        }

        List<String> participantIds = c.getParticipants().stream().map(Utilisateur::getId).toList();

        return ConversationResponseDTO.builder()
                .id(c.getId())
                .dateCreation(c.getDateCreation())
                .statut(c.getStatut())
                .participantIds(participantIds)
                .correspondantId(correspondant != null ? correspondant.getId() : null)
                .correspondantNom(correspondant != null ? correspondant.getNom() : null)
                .correspondantPrenom(correspondant != null ? correspondant.getPrenom() : null)
                .dernierMessage(dernierMsg)
                .build();
    }

    private MessageResponseDTO toMessageDto(MessageContact m) {
        MessageResponseDTO.MessageResponseDTOBuilder builder = MessageResponseDTO.builder()
                .id(m.getId())
                .conversationId(m.getConversation() != null ? m.getConversation().getId() : null)
                .objet(m.getObjet())
                .contenu(m.getContenu())
                .dateEnvoi(m.getDateEnvoi());

        if (m.getExpediteur() != null) {
            builder.expediteurId(m.getExpediteur().getId())
                    .expediteurNom(m.getExpediteur().getNom())
                    .expediteurPrenom(m.getExpediteur().getPrenom());
        }

        return builder.build();
    }
}
