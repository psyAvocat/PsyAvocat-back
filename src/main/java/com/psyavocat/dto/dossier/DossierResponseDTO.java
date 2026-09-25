package com.psyavocat.dto.dossier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DossierResponseDTO {

    private String id;
    private String titre;
    private String description;
    private LocalDate dateOuverture;
    private String statut;
    private String justiciableId;

    private List<PieceJointeDTO> piecesJointes;
    private List<EcheanceDTO> echeances;
    private List<SoumissionDossierResponseDTO> soumissions;
}
