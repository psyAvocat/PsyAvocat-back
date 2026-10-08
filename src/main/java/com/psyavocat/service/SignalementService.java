package com.psyavocat.service;

import com.psyavocat.dto.signalement.CreateSignalementRequest;
import com.psyavocat.dto.signalement.MotifSignalementDTO;
import com.psyavocat.dto.signalement.SignalementCreeDTO;

import java.util.List;

/** Signalement d'un professionnel par un client (le traitement reste dans l'espace Admin). */
public interface SignalementService {

    List<MotifSignalementDTO> getMotifs();

    SignalementCreeDTO signalerProfessionnel(CreateSignalementRequest request);
}
