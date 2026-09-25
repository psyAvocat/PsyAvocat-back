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
public class SendMessageRequest {

    private String objet;

    @NotBlank(message = "Le contenu du message ne peut pas être vide")
    private String contenu;
}
