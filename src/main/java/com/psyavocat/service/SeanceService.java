package com.psyavocat.service;

import com.psyavocat.dto.seance.*;

import java.util.List;

/**
 * Contrat de service pour la gestion des fiches patients, séances et notes de consultation psychologique.
 */
public interface SeanceService {

    List<FichePatientResponseDTO> getMesFichesPatient();

    FichePatientResponseDTO getFichePatient(String patientId);

    SeanceResponseDTO createSeance(CreateSeanceRequest request);

    NoteSeanceDTO ajouterNoteSeance(String seanceId, CreateNoteRequest request);
}
