package com.psyavocat.service.impl;

import com.psyavocat.dto.seance.*;
import com.psyavocat.entity.FichePatient;
import com.psyavocat.entity.NoteSeance;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.entity.Seance;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.SeanceService;
import com.psyavocat.service.support.AccesProfessionnel;
import com.psyavocat.service.support.ClientAccounts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class SeanceServiceImpl implements SeanceService {

    private final FichePatientRepository fichePatientRepository;
    private final SeanceRepository seanceRepository;
    private final NoteSeanceRepository noteSeanceRepository;
    private final PsychologueRepository psychologueRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RendezVousRepository rendezVousRepository;
    private final AuthenticationContext authenticationContext;
    private final AccesProfessionnel accesProfessionnel;

    public SeanceServiceImpl(
            FichePatientRepository fichePatientRepository,
            SeanceRepository seanceRepository,
            NoteSeanceRepository noteSeanceRepository,
            PsychologueRepository psychologueRepository,
            UtilisateurRepository utilisateurRepository,
            RendezVousRepository rendezVousRepository,
            AuthenticationContext authenticationContext,
            AccesProfessionnel accesProfessionnel
    ) {
        this.fichePatientRepository = fichePatientRepository;
        this.seanceRepository = seanceRepository;
        this.noteSeanceRepository = noteSeanceRepository;
        this.psychologueRepository = psychologueRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.rendezVousRepository = rendezVousRepository;
        this.authenticationContext = authenticationContext;
        this.accesProfessionnel = accesProfessionnel;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FichePatientResponseDTO> getMesFichesPatient() {
        String uid = accesProfessionnel.exigerPsychologueApprouve().getId();
        return fichePatientRepository.findByPsychologueIdOrderByDateCreationDesc(uid).stream()
                .map(this::toFicheDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FichePatientResponseDTO getFichePatient(String patientId) {
        String uid = accesProfessionnel.exigerPsychologueApprouve().getId();
        FichePatient fiche = fichePatientRepository.findByPsychologueIdAndPatientId(uid, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche patient introuvable"));
        return toFicheDto(fiche);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeanceResponseDTO> getMesSeances() {
        String uid = accesProfessionnel.exigerPsychologueApprouve().getId();
        return seanceRepository.findByPsychologueIdOrderByDateDesc(uid).stream()
                .map(this::toSeanceDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeanceResponseDTO getSeanceById(String id) {
        String uid = accesProfessionnel.exigerPsychologueApprouve().getId();
        Seance seance = seanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));
        if (!seance.getPsychologue().getId().equals(uid)) {
            throw new ForbiddenException("Accès non autorisé aux notes de cette séance");
        }
        return toSeanceDto(seance);
    }

    @Override
    public SeanceResponseDTO createSeance(CreateSeanceRequest request) {
        Psychologue psychologue = accesProfessionnel.exigerPsychologueApprouve();
        String uid = psychologue.getId();

        Utilisateur patient = utilisateurRepository.findById(request.getPatientId())
                .filter(ClientAccounts::isClient)
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable"));

        // Le rendez-vous rattaché doit être un rendez-vous de CE psychologue avec CE patient.
        RendezVous rdv = null;
        if (request.getRendezVousId() != null && !request.getRendezVousId().isBlank()) {
            rdv = rendezVousRepository.findById(request.getRendezVousId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"));
            boolean rdvDuPsychologue = rdv.getProfessionnel() != null && uid.equals(rdv.getProfessionnel().getId());
            boolean rdvDuPatient = rdv.getPatient() != null && patient.getId().equals(rdv.getPatient().getId());
            if (!rdvDuPsychologue || !rdvDuPatient) {
                throw new ForbiddenException("Ce rendez-vous ne concerne pas ce patient ou n'est pas le vôtre");
            }
            if ("ANNULE".equalsIgnoreCase(rdv.getStatut())) {
                throw new BadRequestException("Une séance ne peut pas être rattachée à un rendez-vous annulé");
            }
        }

        // Un psychologue ne suit que ses propres patients : fiche existante ou rendez-vous commun.
        FichePatient fiche = fichePatientRepository.findByPsychologueIdAndPatientId(uid, patient.getId())
                .orElseGet(() -> {
                    if (!rendezVousRepository.existsByProfessionnelIdAndPatientId(uid, patient.getId())) {
                        throw new ForbiddenException("Ce client n'est pas l'un de vos patients");
                    }
                    FichePatient nouvelleFiche = new FichePatient();
                    nouvelleFiche.setDateCreation(LocalDate.now());
                    nouvelleFiche.setPsychologue(psychologue);
                    nouvelleFiche.setPatient(patient);
                    return fichePatientRepository.save(nouvelleFiche);
                });

        Seance seance = new Seance();
        seance.setDate(request.getDate());
        seance.setStatut(request.getStatut() != null && !request.getStatut().isBlank() ? request.getStatut() : "REALISEE");
        seance.setPsychologue(psychologue);
        seance.setFichePatient(fiche);
        seance.setRendezVousId(rdv != null ? rdv.getId() : null);
        seance.setNotes(new ArrayList<>());

        // La séance réalisée clôt le rendez-vous dont elle découle.
        if (rdv != null && "CONFIRME".equalsIgnoreCase(rdv.getStatut())) {
            rdv.setStatut("PASSE");
            rendezVousRepository.save(rdv);
        }

        if (request.getNoteInitiale() != null && !request.getNoteInitiale().isBlank()) {
            NoteSeance note = new NoteSeance();
            note.setContenu(request.getNoteInitiale());
            note.setDateCreation(LocalDateTime.now());
            note.setSeance(seance);
            seance.getNotes().add(note);
        }

        Seance saved = seanceRepository.save(seance);
        return toSeanceDto(saved);
    }

    @Override
    public NoteSeanceDTO ajouterNoteSeance(String seanceId, CreateNoteRequest request) {
        String uid = accesProfessionnel.exigerPsychologueApprouve().getId();
        Seance seance = seanceRepository.findById(seanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Séance introuvable"));

        if (!seance.getPsychologue().getId().equals(uid)) {
            throw new ForbiddenException("Vous n'êtes pas le psychologue responsable de cette séance");
        }

        NoteSeance note = new NoteSeance();
        note.setContenu(request.getContenu());
        note.setDateCreation(LocalDateTime.now());
        note.setSeance(seance);

        NoteSeance saved = noteSeanceRepository.save(note);
        return new NoteSeanceDTO(saved.getId(), saved.getContenu(), saved.getDateCreation());
    }

    private FichePatientResponseDTO toFicheDto(FichePatient f) {
        List<SeanceResponseDTO> seanceDtos = f.getSeances() != null
                ? f.getSeances().stream().map(this::toSeanceDto).toList()
                : Collections.emptyList();

        FichePatientResponseDTO.FichePatientResponseDTOBuilder builder = FichePatientResponseDTO.builder()
                .id(f.getId())
                .dateCreation(f.getDateCreation())
                .seances(seanceDtos);

        if (f.getPatient() != null) {
            builder.patientId(f.getPatient().getId())
                    .patientNom(f.getPatient().getNom())
                    .patientPrenom(f.getPatient().getPrenom())
                    .patientEmail(f.getPatient().getEmail())
                    .patientTelephone(f.getPatient().getTelephone());
        }

        return builder.build();
    }

    private SeanceResponseDTO toSeanceDto(Seance s) {
        List<NoteSeanceDTO> noteDtos = s.getNotes() != null
                ? s.getNotes().stream()
                    .map(n -> new NoteSeanceDTO(n.getId(), n.getContenu(), n.getDateCreation()))
                    .toList()
                : Collections.emptyList();

        SeanceResponseDTO.SeanceResponseDTOBuilder builder = SeanceResponseDTO.builder()
                .id(s.getId())
                .date(s.getDate())
                .statut(s.getStatut())
                .psychologueId(s.getPsychologue() != null ? s.getPsychologue().getId() : null)
                .fichePatientId(s.getFichePatient() != null ? s.getFichePatient().getId() : null)
                .rendezVousId(s.getRendezVousId())
                .notes(noteDtos);

        if (s.getFichePatient() != null && s.getFichePatient().getPatient() != null) {
            Utilisateur p = s.getFichePatient().getPatient();
            builder.patientId(p.getId())
                   .patientNom(p.getNom())
                   .patientPrenom(p.getPrenom())
                   .patientEmail(p.getEmail())
                   .patientTelephone(p.getTelephone());
        }

        return builder.build();
    }
}
