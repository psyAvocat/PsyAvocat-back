package com.psyavocat.service.impl;

import com.psyavocat.dto.admin.DecisionAdministrativeDTO;
import com.psyavocat.dto.admin.SignalementResponseDTO;
import com.psyavocat.dto.admin.UtilisateurResponseDTO;
import com.psyavocat.entity.Administrateur;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Signalement;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.ClientRepository;
import com.psyavocat.repository.JusticiableRepository;
import com.psyavocat.repository.PatientRepository;
import com.psyavocat.repository.SignalementRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.service.AdminService;
import com.psyavocat.service.NotificationService;
import com.psyavocat.service.ProfessionnelService;
import com.psyavocat.service.notification.NotificationEvenement;
import com.psyavocat.service.support.JournalDecisions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.psyavocat.service.support.Entites;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    static final String SIGNALEMENT_EN_ATTENTE = "EN_ATTENTE";
    static final String SIGNALEMENT_SUSPENDU = "TRAITE_SUSPENDU";
    static final String SIGNALEMENT_CLASSE = "TRAITE_CLASSE";

    private final UtilisateurRepository utilisateurRepository;
    private final SignalementRepository signalementRepository;
    private final PatientRepository patientRepository;
    private final JusticiableRepository justiciableRepository;
    private final ClientRepository clientRepository;
    private final ProfessionnelService professionnelService;
    private final NotificationService notificationService;
    private final JournalDecisions journalDecisions;
    private final RealtimeGateway realtimeGateway;

    public AdminServiceImpl(UtilisateurRepository utilisateurRepository,
                            SignalementRepository signalementRepository,
                            PatientRepository patientRepository,
                            JusticiableRepository justiciableRepository,
                            ClientRepository clientRepository,
                            ProfessionnelService professionnelService,
                            NotificationService notificationService,
                            JournalDecisions journalDecisions,
                            RealtimeGateway realtimeGateway) {
        this.utilisateurRepository = utilisateurRepository;
        this.signalementRepository = signalementRepository;
        this.patientRepository = patientRepository;
        this.justiciableRepository = justiciableRepository;
        this.clientRepository = clientRepository;
        this.professionnelService = professionnelService;
        this.notificationService = notificationService;
        this.journalDecisions = journalDecisions;
        this.realtimeGateway = realtimeGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurResponseDTO> getAllUtilisateurs() {
        // Uniquement les clients (patients / justiciables) : jamais l'administrateur ni les professionnels.
        List<Utilisateur> clients = new ArrayList<>();
        clients.addAll(clientRepository.findAll());
        clients.addAll(patientRepository.findAll());
        clients.addAll(justiciableRepository.findAll());
        return clients.stream()
                .distinct()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurResponseDTO getUtilisateurById(String id) {
        return utilisateurRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));
    }

    /**
     * Activation / désactivation d'un compte : prise en compte immédiate par le filtre de sécurité
     * (403 ACCOUNT_DISABLED) et par les sessions ouvertes (événement temps réel).
     */
    @Override
    public UtilisateurResponseDTO toggleStatutUtilisateur(String id, boolean actif) {
        Utilisateur user = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));
        if (user instanceof Administrateur) {
            throw new BadRequestException("Impossible de modifier le statut d'un administrateur.");
        }
        user.setActif(actif);
        Utilisateur saved = utilisateurRepository.save(user);
        realtimeGateway.envoyerA(saved.getId(), "COMPTE_MIS_A_JOUR", Map.of("actif", actif));
        if (saved instanceof Professionnel) {
            realtimeGateway.diffuser("PROFESSIONNEL_MIS_A_JOUR", Map.of("id", saved.getId()));
        }
        return mapToDTO(saved);
    }

    private UtilisateurResponseDTO mapToDTO(Utilisateur user) {
        return UtilisateurResponseDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .dateInscription(user.getDateInscription())
                .typeUtilisateur(user.getClass().getSimpleName())
                .actif(user.getActif() != null ? user.getActif() : true)
                .build();
    }

    // ------------------------------------------------------------------
    // Signalements
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SignalementResponseDTO> getSignalementsActifs() {
        return getSignalements(SIGNALEMENT_EN_ATTENTE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalementResponseDTO> getSignalements(String statut) {
        List<Signalement> list = statut == null || statut.isBlank()
                ? signalementRepository.findAll()
                : signalementRepository.findByStatut(statut.trim().toUpperCase());
        return list.stream()
                .sorted(Comparator.comparing(Signalement::getDateSignalement,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSignalementDto)
                .toList();
    }

    /**
     * Décision sur un signalement EN_ATTENTE :
     * - SUSPENDRE (motif obligatoire) : suspension du professionnel via le workflow standard
     *   (accès coupés, rendez-vous futurs annulés, notifications) ;
     * - CLASSER : signalement clos sans suite.
     * La décision est historisée et l'auteur du signalement est informé.
     */
    @Override
    public SignalementResponseDTO traiterSignalement(String id, String action, String motif) {
        Signalement s = signalementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable"));
        if (!SIGNALEMENT_EN_ATTENTE.equalsIgnoreCase(s.getStatut())) {
            throw new BadRequestException("Ce signalement a déjà été traité.");
        }
        String decision = action != null ? action.trim().toUpperCase() : "";
        String motifNettoye = motif != null && !motif.isBlank() ? motif.trim() : null;

        switch (decision) {
            case "SUSPENDRE" -> {
                if (motifNettoye == null) {
                    throw new BadRequestException("Le motif est obligatoire pour suspendre un professionnel.");
                }
                if (s.getUtilisateurVise() == null) {
                    throw new BadRequestException("Le professionnel visé n'existe plus.");
                }
                professionnelService.updateStatutValidation(s.getUtilisateurVise().getId(), "SUSPENDED", motifNettoye);
                s.setStatut(SIGNALEMENT_SUSPENDU);
            }
            case "CLASSER" -> s.setStatut(SIGNALEMENT_CLASSE);
            default -> throw new BadRequestException("Action invalide (SUSPENDRE ou CLASSER).");
        }
        Signalement saved = signalementRepository.save(s);
        journalDecisions.enregistrer(JournalDecisions.TYPE_SIGNALEMENT, saved.getId(), saved.getStatut(), motifNettoye);

        if (saved.getAuteur() != null) {
            notificationService.notifier(new NotificationEvenement(
                    saved.getAuteur().getId(),
                    "SIGNALEMENT_TRAITE",
                    "Votre signalement a été traité",
                    "Merci pour votre signalement. L'équipe PsyAvocat l'a examiné et a pris les mesures appropriées.",
                    universDe(saved.getUtilisateurVise()),
                    "SIGNALEMENT",
                    saved.getId(),
                    null));
        }
        return toSignalementDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DecisionAdministrativeDTO> getHistoriqueDecisions(String cibleId) {
        return journalDecisions.historique(cibleId);
    }

    private SignalementResponseDTO toSignalementDto(Signalement s) {
        Utilisateur vise = s.getUtilisateurVise();
        return SignalementResponseDTO.builder()
                .id(s.getId())
                .professionnelCibleId(vise != null ? vise.getId() : null)
                .professionnelCibleNom(vise != null ? vise.getPrenom() + " " + vise.getNom() : "Compte supprimé")
                .auteurNom(s.getAuteur() != null ? s.getAuteur().getPrenom() + " " + s.getAuteur().getNom() : "Compte supprimé")
                .motif(s.getMotif())
                .description(s.getContenuVise())
                .statut(s.getStatut())
                .dateSignalement(s.getDateSignalement())
                .typeProfessionnel(universDe(vise))
                .statutProfessionnel(Entites.reel(vise) instanceof Professionnel pro ? pro.getStatutValidation() : null)
                .build();
    }

    private String universDe(Utilisateur u) {
        u = Entites.reel(u);
        if (u instanceof Avocat) {
            return "AVOCAT";
        }
        return u instanceof Professionnel ? "PSYCHOLOGUE" : null;
    }
}
