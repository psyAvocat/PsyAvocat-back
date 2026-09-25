package com.psyavocat.dto.messagerie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {

    private String id;
    private String conversationId;
    private String expediteurId;
    private String expediteurNom;
    private String expediteurPrenom;
    private String objet;
    private String contenu;
    private LocalDateTime dateEnvoi;
}
