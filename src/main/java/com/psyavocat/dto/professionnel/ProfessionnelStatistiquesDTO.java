package com.psyavocat.dto.professionnel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.psyavocat.dto.admin.PointMensuelDTO;
import com.psyavocat.dto.admin.RepartitionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfessionnelStatistiquesDTO {
    private long totalRendezVous;
    private long rendezVousConfirmes;
    private long rendezVousEffectues;
    private long rendezVousAnnules;
    private long totalClients;
    private BigDecimal honorairesEstimes;
    
    // Graphiques
    private List<PointMensuelDTO> evolutionRendezVous;
    private List<RepartitionDTO> repartitionStatutsRendezVous;
    private List<RepartitionDTO> repartitionModesConsultation;
    private List<RepartitionDTO> repartitionStatutsDossiers; // Rempli si Avocat
}
