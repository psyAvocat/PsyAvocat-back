package com.psyavocat.controller;

import com.psyavocat.dto.messagerie.*;
import com.psyavocat.service.MessagerieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class MessagerieController {

    private final MessagerieService messagerieService;

    public MessagerieController(MessagerieService messagerieService) {
        this.messagerieService = messagerieService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponseDTO>> getMesConversations() {
        return ResponseEntity.ok(messagerieService.getMesConversations());
    }

    /** Discussion existante avec ce destinataire, ou brouillon (sans identifiant) : rien n'est créé. */
    @GetMapping("/avec/{destinataireId}")
    public ResponseEntity<ConversationResponseDTO> ouvrirAvec(@PathVariable String destinataireId) {
        return ResponseEntity.ok(messagerieService.ouvrirAvec(destinataireId));
    }

    @PostMapping
    public ResponseEntity<ConversationResponseDTO> createConversation(
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messagerieService.createConversation(request));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponseDTO>> getMessages(@PathVariable String id) {
        return ResponseEntity.ok(messagerieService.getMessages(id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messagerieService.sendMessage(id, request));
    }

    /** L'expéditeur supprime l'un de ses messages (pour tous les participants). */
    @DeleteMapping("/{id}/messages/{messageId}")
    public ResponseEntity<Void> supprimerMessage(@PathVariable String id, @PathVariable String messageId) {
        messagerieService.supprimerMessage(id, messageId);
        return ResponseEntity.noContent().build();
    }

    /** Supprime la discussion de sa messagerie ; l'autre participant la conserve. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerConversation(@PathVariable String id) {
        messagerieService.supprimerConversation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<java.util.Map<String, Long>> getUnreadMessagesCount() {
        return ResponseEntity.ok(java.util.Map.of("unreadCount", messagerieService.getNombreMessagesNonLus()));
    }
}
