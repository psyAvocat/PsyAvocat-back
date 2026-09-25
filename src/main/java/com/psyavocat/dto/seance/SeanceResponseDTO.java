package com.psyavocat.dto.seance;

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
public class SeanceResponseDTO {

    private String id;
    private LocalDateTime date;
    private String statut;
    private String psychologueId;
    private String fichePatientId;
    private List<NoteSeanceDTO> notes;
}
