package com.psyavocat.service.impl;

import com.psyavocat.dto.admin.UtilisateurResponseDTO;
import com.psyavocat.dto.admin.SignalementResponseDTO;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Signalement;
import com.psyavocat.repository.ProfessionnelRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.repository.SignalementRepository;
import com.psyavocat.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final ProfessionnelRepository professionnelRepository;
    private final SignalementRepository signalementRepository;
    private final com.psyavocat.repository.PatientRepository patientRepository;
    private final com.psyavocat.repository.JusticiableRepository justiciableRepository;

    public AdminServiceImpl(UtilisateurRepository utilisateurRepository,
                            ProfessionnelRepository professionnelRepository,
                            SignalementRepository signalementRepository,
                            com.psyavocat.repository.PatientRepository patientRepository,
                            com.psyavocat.repository.JusticiableRepository justiciableRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.professionnelRepository = professionnelRepository;
        this.signalementRepository = signalementRepository;
        this.patientRepository = patientRepository;
        this.justiciableRepository = justiciableRepository;
    }

    @Override
    public List<UtilisateurResponseDTO> getAllUtilisateurs() {
        // Règle 18 : uniquement les clients/patients (Patient et Justiciable). Jamais l'Admin ni les Pros.
        List<Utilisateur> clients = new ArrayList<>();
        clients.addAll(patientRepository.findAll());
        clients.addAll(justiciableRepository.findAll());
        return clients.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UtilisateurResponseDTO getUtilisateurById(String id) {
        return utilisateurRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new com.psyavocat.exception.ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));
    }

    @Override
    public UtilisateurResponseDTO toggleStatutUtilisateur(String id, boolean actif) {
        Utilisateur user = utilisateurRepository.findById(id)
                .orElseThrow(() -> new com.psyavocat.exception.ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));
        if (user instanceof com.psyavocat.entity.Administrateur) {
            throw new com.psyavocat.exception.BadRequestException("Impossible de modifier le statut d'un administrateur.");
        }
        user.setActif(actif);
        Utilisateur saved = utilisateurRepository.save(user);
        return mapToDTO(saved);
    }

    private UtilisateurResponseDTO mapToDTO(Utilisateur user) {
        String type = user.getClass().getSimpleName();
        return UtilisateurResponseDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .dateInscription(user.getDateInscription())
                .typeUtilisateur(type)
                .actif(user.getActif() != null ? user.getActif() : true)
                .build();
    }

    @Override
    public List<SignalementResponseDTO> getSignalementsActifs() {

        // En supposant que "EN_ATTENTE" est le statut des signalements actifs
        return signalementRepository.findByStatut("EN_ATTENTE").stream().map(s -> {
            String gravite = "Moyen";
            if (s.getMotif() != null && s.getMotif().toLowerCase().contains("déontologique")) gravite = "Urgent";
            
            return SignalementResponseDTO.builder()
                    .id(s.getId())
                    .professionnelCibleId(s.getUtilisateurVise() != null ? s.getUtilisateurVise().getId() : null)
                    .professionnelCibleNom(s.getUtilisateurVise() != null ? s.getUtilisateurVise().getNom() + " " + s.getUtilisateurVise().getPrenom() : "Inconnu")
                    .auteurNom(s.getAuteur() != null ? s.getAuteur().getNom() + " " + s.getAuteur().getPrenom() : "Inconnu")
                    .motif(s.getMotif())
                    .description(s.getContenuVise())
                    .gravite(gravite)
                    .statut(s.getStatut())
                    .dateSignalement(s.getDateSignalement())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public SignalementResponseDTO traiterSignalement(String id, String action) {
        Signalement s = signalementRepository.findById(id).orElseThrow(() -> new RuntimeException("Signalement introuvable"));
        if ("SUSPENDRE".equalsIgnoreCase(action)) {
            Professionnel pro = professionnelRepository.findById(s.getUtilisateurVise().getId()).orElse(null);
            if(pro != null) {
                pro.setStatutValidation("SUSPENDED");
                professionnelRepository.save(pro);
            }
            s.setStatut("TRAITE_SUSPENDU");
        } else if ("CLASSER".equalsIgnoreCase(action)) {
            s.setStatut("TRAITE_CLASSE");
        }
        signalementRepository.save(s);
        
        return SignalementResponseDTO.builder()
                .id(s.getId())
                .statut(s.getStatut())
                .build();
    }
}
