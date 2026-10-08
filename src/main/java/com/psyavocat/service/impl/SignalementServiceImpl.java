package com.psyavocat.service.impl;

import com.psyavocat.dto.signalement.CreateSignalementRequest;
import com.psyavocat.dto.signalement.MotifSignalementDTO;
import com.psyavocat.dto.signalement.SignalementCreeDTO;
import com.psyavocat.entity.Administrateur;
import com.psyavocat.entity.MotifSignalement;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Signalement;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.AdministrateurRepository;
import com.psyavocat.repository.ProfessionnelRepository;
import com.psyavocat.repository.SignalementRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.NotificationService;
import com.psyavocat.service.SignalementService;
import com.psyavocat.service.notification.NotificationEvenement;
import com.psyavocat.service.support.ClientAccounts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class SignalementServiceImpl implements SignalementService {

    /** Statut initial attendu par l'espace Admin (liste des signalements actifs). */
    static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    private static final int DESCRIPTION_MIN_AUTRE = 10;

    private final SignalementRepository signalementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProfessionnelRepository professionnelRepository;
    private final AdministrateurRepository administrateurRepository;
    private final NotificationService notificationService;
    private final AuthenticationContext authenticationContext;

    public SignalementServiceImpl(SignalementRepository signalementRepository,
                                  UtilisateurRepository utilisateurRepository,
                                  ProfessionnelRepository professionnelRepository,
                                  AdministrateurRepository administrateurRepository,
                                  NotificationService notificationService,
                                  AuthenticationContext authenticationContext) {
        this.signalementRepository = signalementRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.professionnelRepository = professionnelRepository;
        this.administrateurRepository = administrateurRepository;
        this.notificationService = notificationService;
        this.authenticationContext = authenticationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MotifSignalementDTO> getMotifs() {
        return Arrays.stream(MotifSignalement.values())
                .map(m -> new MotifSignalementDTO(m.name(), m.getLibelle()))
                .toList();
    }

    @Override
    public SignalementCreeDTO signalerProfessionnel(CreateSignalementRequest request) {
        // L'auteur est TOUJOURS l'utilisateur authentifié : impossible de signaler au nom d'un autre.
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur auteur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));
        if (!ClientAccounts.isClient(auteur)) {
            throw new ForbiddenException("Seul un client peut signaler un professionnel");
        }

        Professionnel cible = professionnelRepository.findById(request.getProfessionnelId())
                .orElseThrow(() -> new ResourceNotFoundException("Professionnel introuvable"));

        String description = request.getDescription() != null ? request.getDescription().trim() : "";
        if (request.getMotif() == MotifSignalement.AUTRE && description.length() < DESCRIPTION_MIN_AUTRE) {
            throw new BadRequestException("Décrivez la situation (au moins " + DESCRIPTION_MIN_AUTRE + " caractères) pour le motif « Autre ».");
        }

        if (signalementRepository.existsByAuteurIdAndUtilisateurViseIdAndStatut(uid, cible.getId(), STATUT_EN_ATTENTE)) {
            throw new ConflictException("Vous avez déjà un signalement en cours d'examen pour ce professionnel.");
        }

        Signalement signalement = new Signalement();
        signalement.setAuteur(auteur);
        signalement.setUtilisateurVise(cible);
        signalement.setMotif(request.getMotif().getLibelle());
        signalement.setContenuVise(description.isEmpty() ? null : description);
        signalement.setStatut(STATUT_EN_ATTENTE);
        signalement.setDateSignalement(LocalDateTime.now());
        Signalement saved = signalementRepository.save(signalement);

        for (Administrateur admin : administrateurRepository.findAll()) {
            notificationService.notifier(new NotificationEvenement(
                    admin.getId(),
                    "SIGNALEMENT_NOUVEAU",
                    "Nouveau signalement",
                    "Signalement de " + cible.getPrenom() + " " + cible.getNom() + " : " + request.getMotif().getLibelle() + ".",
                    null,
                    "SIGNALEMENT",
                    saved.getId(),
                    null));
        }

        return new SignalementCreeDTO(saved.getId(), saved.getStatut(), saved.getDateSignalement());
    }
}
