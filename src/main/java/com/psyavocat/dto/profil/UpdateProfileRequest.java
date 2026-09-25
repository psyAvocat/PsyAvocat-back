package com.psyavocat.dto.profil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String nom;
    private String prenom;
    private String telephone;

    // Pour les professionnels
    private String biographie;
    private String ville;
    private String adresse;
    private String modeConsultation;
    private List<String> specialiteIds;
}
