package com.psyavocat.dto.professionnel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfessionnelResponseDTO {

    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String type; // AVOCAT, PSYCHOLOGUE
    private String biographie;
    private String ville;
    private String adresse;
    private String modeConsultation;
    private String statutValidation; // PENDING, APPROVED, REJECTED, SUSPENDED
    private String numeroBarreau;
    private String numeroAgrement;
    private List<SpecialiteDTO> specialites;
}
