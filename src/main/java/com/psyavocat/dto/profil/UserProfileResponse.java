package com.psyavocat.dto.profil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
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
public class UserProfileResponse {

    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String typeUtilisateur; // PATIENT, JUSTICIABLE, AVOCAT, PSYCHOLOGUE, ADMINISTRATEUR
    private LocalDate dateInscription;

    // Champs spécifiques professionnels
    private String statutValidation; // PENDING, APPROVED, REJECTED, SUSPENDED
    private String biographie;
    private String ville;
    private String adresse;
    private String modeConsultation;
    private List<SpecialiteDTO> specialites;

    // Spécifique Avocat
    private String numeroBarreau;

    // Spécifique Psychologue
    private String numeroAgrement;
}
