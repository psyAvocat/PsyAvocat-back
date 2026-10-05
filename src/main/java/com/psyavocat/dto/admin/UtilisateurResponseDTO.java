package com.psyavocat.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponseDTO {
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateInscription;
    private String typeUtilisateur; // Patient, Justiciable
    private Boolean actif;
}

