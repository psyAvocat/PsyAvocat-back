package com.psyavocat.dto.messagerie;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotBlank(message = "L'identifiant du destinataire est obligatoire")
    private String destinataireId;

    private String objet;

    @NotBlank(message = "Le premier message ne peut pas être vide")
    private String premierMessage;
}
