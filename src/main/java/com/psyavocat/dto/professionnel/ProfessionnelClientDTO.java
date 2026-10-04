package com.psyavocat.dto.professionnel;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfessionnelClientDTO {
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDateTime dernierRendezVous;
    private long nombreRendezVous;
    private String statut; // ACTIF, NOUVEAU, HISTORIQUE
    private String dernierMode; // VISIO, CABINET
    private Integer dossiersCount; // Pour Avocat
    private Integer seancesCount;  // Pour Psychologue
}
