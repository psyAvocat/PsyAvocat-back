package com.psyavocat.service;

import com.psyavocat.dto.rendezvous.CreateRendezVousAvocatRequest;
import com.psyavocat.dto.rendezvous.CreateRendezVousPsyRequest;
import com.psyavocat.dto.rendezvous.RendezVousResponseDTO;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RendezVousService {

    private static final BigDecimal TAUX_ACOMPTE = new BigDecimal("0.20");

    private final RendezVousRepository rendezVousRepository;
    private final DisponibiliteRepository disponibiliteRepository;
    private final PsychologueRepository psychologueRepository;
    private final SoumissionDossierRepository soumissionDossierRepository;
    private final PaiementRepository paiementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;

    public RendezVousService(
            RendezVousRepository rendezVousRepository,
            DisponibiliteRepository disponibiliteRepository,
            PsychologueRepository psychologueRepository,
            SoumissionDossierRepository soumissionDossierRepository,
            PaiementRepository paiementRepository,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext
    ) {
        this.rendezVousRepository = rendezVousRepository;
        this.disponibiliteRepository = disponibiliteRepository;
        this.psychologueRepository = psychologueRepository;
        this.soumissionDossierRepository = soumissionDossierRepository;
        this.paiementRepository = paiementRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.authenticationContext = authenticationContext;
    }

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

        disp.setStatut("RESERVE");
        disponibiliteRepository.save(disp);

        BigDecimal montantTotal = request.getMontantTotal();
        BigDecimal montantAcompte = montantTotal.multiply(TAUX_ACOMPTE).setScale(2, RoundingMode.HALF_UP);

        RendezVous rdv = new RendezVous();
        rdv.setDateHeure(LocalDateTime.of(disp.getDate(), disp.getHeureDebut()));
        rdv.setStatut("CONFIRME");
        rdv.setMode(request.getMode() != null ? request.getMode() : "VISIO");
        rdv.setPatient(patient);
        rdv.setProfessionnel(psychologue);

        RendezVous savedRdv = rendezVousRepository.save(rdv);

        Paiement paiement = new Paiement();
        paiement.setMontant(montantAcompte);
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setStatut("PAYE");
        paiement.setMethode("SIMULATION_CARTE");
        paiement.setUtilisateur(patient);
        paiement.setRendezVous(savedRdv);
        paiementRepository.save(paiement);

        savedRdv.setPaiement(paiement);
        return toDto(savedRdv, montantTotal);
    }

    public RendezVousResponseDTO createRendezVousAvocat(CreateRendezVousAvocatRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur justiciable = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));

        SoumissionDossier soumission = soumissionDossierRepository.findById(request.getSoumissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Soumission de dossier introuvable"));

        if (!"ACCEPTEE".equalsIgnoreCase(soumission.getStatut())) {
            throw new BadRequestException("Un rendez-vous avocat nécessite que la soumission du dossier soit acceptée");
        }

        if (!soumission.getDossier().getJusticiable().getId().equals(uid)) {
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

        disp.setStatut("RESERVE");
        disponibiliteRepository.save(disp);

        BigDecimal montantTotal = soumission.getTarifPropose() != null ? soumission.getTarifPropose() : BigDecimal.ZERO;
        BigDecimal montantAcompte = montantTotal.multiply(TAUX_ACOMPTE).setScale(2, RoundingMode.HALF_UP);

        RendezVous rdv = new RendezVous();
        rdv.setDateHeure(LocalDateTime.of(disp.getDate(), disp.getHeureDebut()));
        rdv.setStatut("CONFIRME");
        rdv.setMode(request.getMode() != null ? request.getMode() : "CABINET");
        rdv.setPatient(justiciable);
        rdv.setProfessionnel(avocat);

        RendezVous savedRdv = rendezVousRepository.save(rdv);

        Paiement paiement = new Paiement();
        paiement.setMontant(montantAcompte);
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setStatut("PAYE");
        paiement.setMethode("SIMULATION_CARTE");
        paiement.setUtilisateur(justiciable);
        paiement.setRendezVous(savedRdv);
        paiementRepository.save(paiement);

        savedRdv.setPaiement(paiement);
        return toDto(savedRdv, montantTotal);
    }

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
