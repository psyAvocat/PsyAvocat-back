package com.psyavocat.dto.seance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteSeanceDTO {
    private String id;
    private String contenu;
    private LocalDateTime dateCreation;
}
