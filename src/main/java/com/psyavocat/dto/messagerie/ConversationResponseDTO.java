package com.psyavocat.dto.messagerie;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationResponseDTO {

    private String id;
    private LocalDateTime dateCreation;
    private String statut;
    private List<String> participantIds;
    private String correspondantId;
    private String correspondantNom;
    private String correspondantPrenom;
    private MessageResponseDTO dernierMessage;
}
