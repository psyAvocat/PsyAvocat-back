package com.psyavocat.service;

import com.psyavocat.dto.professionnel.ProfessionnelClientDTO;
import com.psyavocat.dto.professionnel.ProfessionnelStatistiquesDTO;

import java.util.List;

public interface ProfessionnelEspaceService {

    List<ProfessionnelClientDTO> getMesClients();

    ProfessionnelStatistiquesDTO getMesStatistiques(int mois);
}
