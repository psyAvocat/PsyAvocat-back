package com.psyavocat.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private String id;
    private String type;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private String lienVisio;
    private boolean lu;
    private String titre;
    private LocalDateTime dateLecture;
    private String univers;
    private String ressourceType;
    private String ressourceId;
}
