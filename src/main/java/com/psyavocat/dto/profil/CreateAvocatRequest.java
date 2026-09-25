package com.psyavocat.dto.profil;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAvocatRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    private String telephone;

    private String biographie;

    private String ville;

    private String adresse;

    private String modeConsultation;

    @NotBlank(message = "Le numéro de barreau est obligatoire")
    private String numeroBarreau;

    @Builder.Default
    private List<String> specialiteIds = new ArrayList<>();
}
