package com.psyavocat.service.impl;

import com.psyavocat.dto.rendezvous.CreateRendezVousAvocatDirectRequest;
import com.psyavocat.dto.rendezvous.CreateRendezVousAvocatRequest;
import com.psyavocat.dto.rendezvous.CreateRendezVousPsyRequest;
import com.psyavocat.dto.rendezvous.RendezVousResponseDTO;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.PaiementService;
import com.psyavocat.service.RendezVousService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RendezVousServiceImpl implements RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final DisponibiliteRepository disponibiliteRepository;
    private final PsychologueRepository psychologueRepository;
    private final AvocatRepository avocatRepository;
    private final SoumissionDossierRepository soumissionDossierRepository;
    private final DossierRepository dossierRepository;
    private final PaiementService paiementService;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;

    public RendezVousServiceImpl(
            RendezVousRepository rendezVousRepository,
            DisponibiliteRepository disponibiliteRepository,
            PsychologueRepository psychologueRepository,
            AvocatRepository avocatRepository,
            SoumissionDossierRepository soumissionDossierRepository,
            DossierRepository dossierRepository,
            PaiementService paiementService,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext
    ) {
        this.rendezVousRepository = rendezVousRepository;
        this.disponibiliteRepository = disponibiliteRepository;
        this.psychologueRepository = psychologueRepository;
        this.avocatRepository = avocatRepository;
        this.soumissionDossierRepository = soumissionDossierRepository;
        this.dossierRepository = dossierRepository;
        this.paiementService = paiementService;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public RendezVousResponseDTO createRendezVousPsychologue(CreateRendezVousPsyRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur patient = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        Psychologue psychologue = psychologueRepository.findById(request.getPsychologueId())
                .orElseThrow(() -> new ResourceNotFoundException("Psychologue introuvable"));

        if (!"APPROVED".equalsIgnoreCase(psychologue.getStatutValidation())) {
            throw new BadRequestException("Ce professionnel n'est pas encore validé par la plateforme");
        }

        Disponibilite disp = disponibiliteRepository.findById(request.getDisponibiliteId())
                .orElseThrow(() -> new ResourceNotFoundException("Créneau de disponibilité introuvable"));

        if (!"LIBRE".equalsIgnoreCase(disp.getStatut())) {
            throw new BadRequestException("Ce créneau n'est plus disponible");
        }

        if (!disp.getProfessionnel().getId().equals(psychologue.getId())) {
            throw new BadRequestException("Le créneau n'appartient pas au psychologue sélectionné");
        }

        // Réservation immédiate du créneau pour prévenir les conflits concurrents
        disp.setStatut("RESERVE");
        disponibiliteRepository.save(disp);

        BigDecimal montantTotal = request.getMontantTotal();
        BigDecimal montantAcompte = paiementService.calculerAcompteRendezVous(montantTotal);

        RendezVous rdv = new RendezVous();
        rdv.setDateHeure(LocalDateTime.of(disp.getDate(), disp.getHeureDebut()));
        rdv.setStatut("CONFIRME");
        rdv.setMode(request.getMode() != null ? request.getMode() : "VISIO");
        rdv.setPatient(patient);
        rdv.setProfessionnel(psychologue);

        RendezVous savedRdv = rendezVousRepository.save(rdv);

        // Traitement de l'acompte simulé via le service de paiement dédié
        Paiement paiement = paiementService.traiterAcompteRendezVous(patient, savedRdv, montantAcompte);
        savedRdv.setPaiement(paiement);

        return toDto(savedRdv, montantTotal);
    }

    @Override
    public RendezVousResponseDTO createRendezVousAvocat(CreateRendezVousAvocatRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur justiciable = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        SoumissionDossier soumission = soumissionDossierRepository.findById(request.getSoumissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Soumission de dossier introuvable"));

        if (!"ACCEPTEE".equalsIgnoreCase(soumission.getStatut())) {
            throw new BadRequestException("Un rendez-vous avocat nécessite que la soumission du dossier soit acceptée");
        }

        Dossier dossier = soumission.getDossier();
        if (dossier != null && "PRIS_EN_CHARGE".equalsIgnoreCase(dossier.getStatut())) {
            throw new BadRequestException("Ce dossier juridique a déjà été pris en charge par un autre avocat");
        }

        if (dossier == null || !dossier.getJusticiable().getId().equals(uid)) {
            throw new ForbiddenException("Vous n'êtes pas le propriétaire de ce dossier");
        }

        Avocat avocat = soumission.getAvocat();
        Disponibilite disp = disponibiliteRepository.findById(request.getDisponibiliteId())
                .orElseThrow(() -> new ResourceNotFoundException("Créneau de disponibilité introuvable"));

        if (!"LIBRE".equalsIgnoreCase(disp.getStatut())) {
            throw new BadRequestException("Ce créneau n'est plus disponible");
        }

        if (!disp.getProfessionnel().getId().equals(avocat.getId())) {
            throw new BadRequestException("Le créneau n'appartient pas à l'avocat ayant accepté le dossier");
        }

        // Réservation immédiate du créneau
        disp.setStatut("RESERVE");
        disponibiliteRepository.save(disp);

        BigDecimal montantTotal = soumission.getTarifPropose() != null ? soumission.getTarifPropose() : BigDecimal.ZERO;
        BigDecimal montantAcompte = paiementService.calculerAcompteRendezVous(montantTotal);

        RendezVous rdv = new RendezVous();
        rdv.setDateHeure(LocalDateTime.of(disp.getDate(), disp.getHeureDebut()));
        rdv.setStatut("CONFIRME");
        rdv.setMode(request.getMode() != null ? request.getMode() : "CABINET");
        rdv.setPatient(justiciable);
        rdv.setProfessionnel(avocat);

        RendezVous savedRdv = rendezVousRepository.save(rdv);

        // Traitement de l'acompte simulé via le service de paiement dédié
        Paiement paiement = paiementService.traiterAcompteRendezVous(justiciable, savedRdv, montantAcompte);
        savedRdv.setPaiement(paiement);

        // RÈGLE MÉTIER (SOLUTION 2) : Verrouillage exclusif du dossier
        // 1. La soumission de l'avocat choisi est RETENUE
        soumission.setStatut("RETENUE");
        soumissionDossierRepository.save(soumission);

        // 2. Le dossier passe à l'état PRIS_EN_CHARGE
        dossier.setStatut("PRIS_EN_CHARGE");
        dossierRepository.save(dossier);

        // 3. Auto-invalidation de toutes les autres soumissions concurrentes pour ce dossier (statut CADUQUE)
        List<SoumissionDossier> autresSoumissions = soumissionDossierRepository.findByDossierId(dossier.getId());
        for (SoumissionDossier autre : autresSoumissions) {
            if (!autre.getId().equals(soumission.getId())) {
                autre.setStatut("CADUQUE");
                soumissionDossierRepository.save(autre);
            }
        }

        return toDto(savedRdv, montantTotal);
    }

    @Override
    public RendezVousResponseDTO createRendezVousAvocatDirect(CreateRendezVousAvocatDirectRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur justiciable = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        Avocat avocat = avocatRepository.findById(request.getAvocatId())
                .orElseThrow(() -> new ResourceNotFoundException("Avocat introuvable"));

        if (!"APPROVED".equalsIgnoreCase(avocat.getStatutValidation()) && !"VALIDE".equalsIgnoreCase(avocat.getStatutValidation())) {
            throw new BadRequestException("Cet avocat n'est pas encore validé par la plateforme");
        }

        Disponibilite disp = disponibiliteRepository.findById(request.getDisponibiliteId())
                .orElseThrow(() -> new ResourceNotFoundException("Créneau de disponibilité introuvable"));

        if (!"LIBRE".equalsIgnoreCase(disp.getStatut())) {
            throw new BadRequestException("Ce créneau n'est plus disponible");
        }

        if (!disp.getProfessionnel().getId().equals(avocat.getId())) {
            throw new BadRequestException("Le créneau n'appartient pas à l'avocat sélectionné");
        }

        // Réservation immédiate du créneau
        disp.setStatut("RESERVE");
        disponibiliteRepository.save(disp);

        BigDecimal montantTotal = request.getMontantTotal();
        BigDecimal montantAcompte = paiementService.calculerAcompteRendezVous(montantTotal);

        RendezVous rdv = new RendezVous();
        rdv.setDateHeure(LocalDateTime.of(disp.getDate(), disp.getHeureDebut()));
        rdv.setStatut("CONFIRME");
        rdv.setMode(request.getMode() != null ? request.getMode() : "CABINET");
        rdv.setPatient(justiciable);
        rdv.setProfessionnel(avocat);

        RendezVous savedRdv = rendezVousRepository.save(rdv);

        // Traitement de l'acompte simulé via le service de paiement dédié
        Paiement paiement = paiementService.traiterAcompteRendezVous(justiciable, savedRdv, montantAcompte);
        savedRdv.setPaiement(paiement);

        return toDto(savedRdv, montantTotal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RendezVousResponseDTO> getMyRendezVous() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur user = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<RendezVous> list;
        if (user instanceof Professionnel) {
            list = rendezVousRepository.findByProfessionnelIdOrderByDateHeureDesc(uid);
        } else {
            list = rendezVousRepository.findByPatientIdOrderByDateHeureDesc(uid);
        }

        return list.stream().map(r -> toDto(r, null)).toList();
    }

    @Override
    public RendezVousResponseDTO annulerRendezVous(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        RendezVous rdv = rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"));

        boolean isPatient = rdv.getPatient() != null && rdv.getPatient().getId().equals(uid);
        boolean isPro = rdv.getProfessionnel() != null && rdv.getProfessionnel().getId().equals(uid);

        if (!isPatient && !isPro) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à annuler ce rendez-vous");
        }

        rdv.setStatut("ANNULE");
        RendezVous saved = rendezVousRepository.save(rdv);
        return toDto(saved, null);
    }

    private RendezVousResponseDTO toDto(RendezVous rdv, BigDecimal forcedTotal) {
        RendezVousResponseDTO.RendezVousResponseDTOBuilder builder = RendezVousResponseDTO.builder()
                .id(rdv.getId())
                .dateHeure(rdv.getDateHeure())
                .statut(rdv.getStatut())
                .mode(rdv.getMode());

        if (rdv.getPatient() != null) {
            builder.patientId(rdv.getPatient().getId())
                    .patientNom(rdv.getPatient().getNom())
                    .patientPrenom(rdv.getPatient().getPrenom());
        }

        if (rdv.getProfessionnel() != null) {
            builder.professionnelId(rdv.getProfessionnel().getId())
                    .professionnelNom(rdv.getProfessionnel().getNom())
                    .professionnelPrenom(rdv.getProfessionnel().getPrenom())
                    .typeProfessionnel(rdv.getProfessionnel() instanceof Avocat ? "AVOCAT" : "PSYCHOLOGUE");
        }

        if (rdv.getPaiement() != null) {
            builder.montantAcompte(rdv.getPaiement().getMontant())
                    .statutPaiement(rdv.getPaiement().getStatut());
            if (forcedTotal != null) {
                builder.montantTotal(forcedTotal);
            }
        }

        return builder.build();
    }
}
