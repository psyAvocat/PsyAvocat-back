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
}
