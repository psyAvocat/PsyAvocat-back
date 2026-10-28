package com.psyavocat.service.impl;

import com.psyavocat.dto.rendezvous.CreateRendezVousAvocatRequest;
import com.psyavocat.dto.rendezvous.CreateRendezVousPsyRequest;
import com.psyavocat.dto.rendezvous.ModifierCreneauRequest;
import com.psyavocat.dto.rendezvous.RendezVousResponseDTO;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.mapper.MediaUrlResolver;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.MessagerieService;
import com.psyavocat.service.PaiementService;
import com.psyavocat.service.RendezVousService;
import com.psyavocat.service.notification.RendezVousEvenements;
import com.psyavocat.service.support.AccesProfessionnel;
import com.psyavocat.service.support.ClientAccounts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import com.psyavocat.service.support.Entites;

@Service
@Transactional
public class RendezVousServiceImpl implements RendezVousService {

    private static final String CRENEAU_LIBRE = "LIBRE";
    private static final String CRENEAU_RESERVE = "RESERVE";
    private static final Set<String> MODES_AUTORISES = Set.of("VISIO", "CABINET");
    private static final Set<String> STATUTS_MODIFIABLES = Set.of("CONFIRME", "EN_ATTENTE");
    private static final int DUREE_PAR_DEFAUT_MINUTES = 45;

    private final RendezVousRepository rendezVousRepository;
    private final DisponibiliteRepository disponibiliteRepository;
    private final PsychologueRepository psychologueRepository;
    private final AvocatRepository avocatRepository;
    private final SoumissionDossierRepository soumissionDossierRepository;
    private final DossierRepository dossierRepository;
    private final PaiementService paiementService;
    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationContext authenticationContext;
    private final RendezVousEvenements evenements;
    private final MediaUrlResolver mediaUrlResolver;
    private final MessagerieService messagerieService;

    public RendezVousServiceImpl(
            RendezVousRepository rendezVousRepository,
            DisponibiliteRepository disponibiliteRepository,
            PsychologueRepository psychologueRepository,
            AvocatRepository avocatRepository,
            SoumissionDossierRepository soumissionDossierRepository,
            DossierRepository dossierRepository,
            PaiementService paiementService,
            UtilisateurRepository utilisateurRepository,
            AuthenticationContext authenticationContext,
            RendezVousEvenements evenements,
            MediaUrlResolver mediaUrlResolver,
            MessagerieService messagerieService
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
        this.evenements = evenements;
        this.mediaUrlResolver = mediaUrlResolver;
        this.messagerieService = messagerieService;
    }

    // ------------------------------------------------------------------
    // Réservation
    // ------------------------------------------------------------------

    @Override
    public RendezVousResponseDTO createRendezVousPsychologue(CreateRendezVousPsyRequest request) {
        Utilisateur client = clientConnecte();

        Psychologue psychologue = psychologueRepository.findById(request.getPsychologueId())
                .orElseThrow(() -> new ResourceNotFoundException("Psychologue introuvable"));
        verifierProfessionnelValide(psychologue);
        BigDecimal montantTotal = verifierTarif(psychologue, request.getMontantTotal());

        return reserver(client, psychologue, request.getDisponibiliteId(),
                modeOuDefaut(request.getMode(), "VISIO"), montantTotal, request.getMotif(), null);
    }

    /**
     * Rendez-vous avec un avocat : uniquement après acceptation du dossier par cet avocat
     * (workflow dossier → soumission → prise en charge → réservation).
     * Le montant est celui proposé par l'avocat à l'acceptation (donnée serveur).
     */
    @Override
    public RendezVousResponseDTO createRendezVousAvocat(CreateRendezVousAvocatRequest request) {
        Utilisateur client = clientConnecte();

        SoumissionDossier soumission = soumissionDossierRepository.findById(request.getSoumissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Soumission de dossier introuvable"));

        Dossier dossier = soumission.getDossier();
        if (dossier != null && dossier.getJusticiable() != null
                && !dossier.getJusticiable().getId().equals(client.getId())) {
            throw new ForbiddenException("Vous n'êtes pas le propriétaire de ce dossier");
        }
        if ("CADUQUE".equalsIgnoreCase(soumission.getStatut())) {
            throw new BadRequestException("Ce dossier juridique a déjà été pris en charge par un autre avocat");
        }
        if (!"ACCEPTEE".equalsIgnoreCase(soumission.getStatut()) && !"RETENUE".equalsIgnoreCase(soumission.getStatut())) {
            throw new BadRequestException("Un rendez-vous avocat nécessite que la soumission du dossier soit acceptée");
        }
        if (dossier == null || dossier.getJusticiable() == null) {
            throw new ForbiddenException("Vous n'êtes pas le propriétaire de ce dossier");
        }
        if (!AccesProfessionnel.estApprouve(soumission.getAvocat())) {
            throw new BadRequestException("Cet avocat n'est plus disponible sur la plateforme");
        }

        BigDecimal montantTotal = soumission.getTarifPropose();
        if (montantTotal == null || montantTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("L'avocat n'a pas encore communiqué d'honoraires pour ce dossier");
        }
        RendezVousResponseDTO dto = reserver(client, soumission.getAvocat(), request.getDisponibiliteId(),
                modeOuDefaut(request.getMode(), "CABINET"), montantTotal, null, soumission);

        // L'avocat ayant accepté est retenu ; la prise en charge exclusive est (ré)affirmée
        // pour les dossiers acceptés avant l'application de cette règle à l'acceptation.
        if (!"RETENUE".equalsIgnoreCase(soumission.getStatut())) {
            soumission.setStatut("RETENUE");
            soumissionDossierRepository.save(soumission);
        }
        if (!"PRIS_EN_CHARGE".equalsIgnoreCase(dossier.getStatut())) {
            dossier.setStatut("PRIS_EN_CHARGE");
            dossierRepository.save(dossier);
        }
        for (SoumissionDossier autre : soumissionDossierRepository.findByDossierId(dossier.getId())) {
            if (!autre.getId().equals(soumission.getId()) && !"CADUQUE".equalsIgnoreCase(autre.getStatut())) {
                autre.setStatut("CADUQUE");
                soumissionDossierRepository.save(autre);
            }
        }
        return dto;
    }

    /**
     * Réservation commune : le créneau est relu SOUS VERROU EXCLUSIF puis revérifié.
     * Deux réservations simultanées du même créneau sont donc sérialisées par la base :
     * la seconde voit le créneau RESERVE et reçoit une erreur fonctionnelle.
     */
    private RendezVousResponseDTO reserver(Utilisateur client, Professionnel pro, String disponibiliteId,
                                           String mode, BigDecimal montantTotal, String motif,
                                           SoumissionDossier soumission) {
        Disponibilite disp = creneauLibreVerrouille(disponibiliteId, pro);
        verifierClientDisponible(client, disp, null);

        disp.setStatut(CRENEAU_RESERVE);
        disponibiliteRepository.save(disp);

        BigDecimal montantAcompte = paiementService.calculerAcompteRendezVous(montantTotal);

        RendezVous rdv = new RendezVous();
        rdv.setDateHeure(debutDe(disp));
        rdv.setDureeMinutes(dureeDe(disp));
        rdv.setStatut("CONFIRME");
        rdv.setMode(mode);
        rdv.setMotif(motif != null && !motif.isBlank() ? motif.trim() : null);
        rdv.setMontantTotal(montantTotal);
        rdv.setMontantAcompte(montantAcompte);
        rdv.setAdresseCabinet("CABINET".equals(mode) ? pro.getAdresse() : null);
        rdv.setLienVisio("VISIO".equalsIgnoreCase(mode) ? genererLienVisioPsyAvocat(disp.getId()) : null);
        rdv.setPatient(client);
        rdv.setProfessionnel(pro);
        rdv.setDisponibilite(disp);
        rdv.setSoumission(soumission);

        RendezVous savedRdv = rendezVousRepository.save(rdv);

        // Acompte de 20 % via le mécanisme de paiement existant.
        Paiement paiement = paiementService.traiterAcompteRendezVous(client, savedRdv, montantAcompte);
        savedRdv.setPaiement(paiement);

        // Garantit que le client apparaît immédiatement dans la liste de messagerie du professionnel (et inversement).
        messagerieService.assurerConversationEntre(client, pro);
        if ("VISIO".equalsIgnoreCase(mode)) {
            messagerieService.envoyerMessageVisioRendezVous(savedRdv, false);
        }

        evenements.rendezVousConfirme(savedRdv);
        evenements.creneauMisAJour(disp);
        return toDto(savedRdv);
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @Override
    public List<RendezVousResponseDTO> getMyRendezVous() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur user = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<RendezVous> list = user instanceof Professionnel
                ? rendezVousRepository.findByProfessionnelIdOrderByDateHeureDesc(uid)
                : rendezVousRepository.findByClientIdOrderByDateHeureDesc(uid);

        // Un rendez-vous confirmé devient PASSE une fois TERMINÉ (fin = début + durée) :
        // entre son début et sa fin, il est « en cours ».
        LocalDateTime now = LocalDateTime.now();
        for (RendezVous r : list) {
            boolean modifie = false;
            if ("CONFIRME".equalsIgnoreCase(r.getStatut()) && finDe(r) != null && finDe(r).isBefore(now)) {
                r.setStatut("PASSE");
                modifie = true;
            }
            if ("VISIO".equalsIgnoreCase(r.getMode()) && doitGenererLienVisio(r.getLienVisio())) {
                r.setLienVisio(genererLienVisioPsyAvocat(r.getId()));
                modifie = true;
            }
            if (modifie) {
                rendezVousRepository.save(r);
            }
        }

        return list.stream().map(this::toDto).toList();
    }

    @Override
    public RendezVousResponseDTO getRendezVous(String id) {
        RendezVous rdv = rendezVousDuParticipant(id);
        if ("VISIO".equalsIgnoreCase(rdv.getMode()) && doitGenererLienVisio(rdv.getLienVisio())) {
            rdv.setLienVisio(genererLienVisioPsyAvocat(rdv.getId()));
            rendezVousRepository.save(rdv);
        }
        return toDto(rdv);
    }

    // ------------------------------------------------------------------
    // Modification / annulation
    // ------------------------------------------------------------------

    @Override
    public RendezVousResponseDTO modifierCreneau(String id, ModifierCreneauRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        RendezVous rdv = rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"));

        if (rdv.getPatient() == null || !rdv.getPatient().getId().equals(uid)) {
            throw new ForbiddenException("Seul le client peut déplacer ce rendez-vous");
        }
        if (!STATUTS_MODIFIABLES.contains(rdv.getStatut()) || !rdv.getDateHeure().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Ce rendez-vous ne peut plus être modifié");
        }
        Disponibilite ancien = rdv.getDisponibilite();
        if (ancien != null && ancien.getId().equals(request.getDisponibiliteId())) {
            throw new BadRequestException("Ce rendez-vous est déjà positionné sur ce créneau");
        }

        Disponibilite nouveau = creneauLibreVerrouille(request.getDisponibiliteId(), rdv.getProfessionnel());
        verifierClientDisponible(rdv.getPatient(), nouveau, rdv.getId());
        nouveau.setStatut(CRENEAU_RESERVE);
        disponibiliteRepository.save(nouveau);

        if (ancien != null) {
            ancien.setStatut(CRENEAU_LIBRE);
            disponibiliteRepository.save(ancien);
        }

        rdv.setDisponibilite(nouveau);
        rdv.setDateHeure(debutDe(nouveau));
        rdv.setDureeMinutes(dureeDe(nouveau));
        if ("VISIO".equalsIgnoreCase(rdv.getMode()) && doitGenererLienVisio(rdv.getLienVisio())) {
            rdv.setLienVisio(genererLienVisioPsyAvocat(rdv.getId()));
        }
        RendezVous saved = rendezVousRepository.save(rdv);

        if ("VISIO".equalsIgnoreCase(saved.getMode())) {
            messagerieService.envoyerMessageVisioRendezVous(saved, true);
        }

        evenements.rendezVousModifie(saved);
        evenements.creneauMisAJour(nouveau);
        evenements.creneauMisAJour(ancien);
        return toDto(saved);
    }

    @Override
    public RendezVousResponseDTO annulerRendezVous(String id) {
        return annulerRendezVous(id, null);
    }

    @Override
    public RendezVousResponseDTO annulerRendezVous(String id, String motif) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        RendezVous rdv = rendezVousDuParticipant(id);

        if ("ANNULE".equalsIgnoreCase(rdv.getStatut()) || "PASSE".equalsIgnoreCase(rdv.getStatut())) {
            throw new BadRequestException("Ce rendez-vous ne peut plus être annulé");
        }
        // Une fois commencé, le rendez-vous a eu lieu (ou est en cours) : plus d'annulation.
        if (rdv.getDateHeure() != null && !rdv.getDateHeure().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Ce rendez-vous a déjà commencé : il ne peut plus être annulé.");
        }
        String motifNettoye = motif != null && !motif.isBlank() ? motif.trim() : null;
        if (motifNettoye != null && motifNettoye.length() > 300) {
            throw new BadRequestException("Le motif d'annulation ne doit pas dépasser 300 caractères.");
        }
        // Côté praticien, l'annulation est une action métier : compte approuvé obligatoire.
        if (rdv.getProfessionnel() != null && rdv.getProfessionnel().getId().equals(uid)
                && !AccesProfessionnel.estApprouve(rdv.getProfessionnel())) {
            throw new ForbiddenException("Votre compte professionnel ne permet pas cette action actuellement.",
                    AccesProfessionnel.CODE_NON_APPROUVE);
        }

        rdv.setStatut("ANNULE");
        // L'acompte simulé suit le rendez-vous : il est marqué annulé (aucun flux financier réel).
        if (rdv.getPaiement() != null && "PAYE".equalsIgnoreCase(rdv.getPaiement().getStatut())) {
            rdv.getPaiement().setStatut("ANNULE");
        }

        // Si le créneau est encore dans le futur, il redevient réservable.
        Disponibilite d = rdv.getDisponibilite();
        if (d != null && debutDe(d).isAfter(LocalDateTime.now())) {
            d.setStatut(CRENEAU_LIBRE);
            disponibiliteRepository.save(d);
        }

        RendezVous saved = rendezVousRepository.save(rdv);
        evenements.rendezVousAnnule(saved, uid, motifNettoye);
        evenements.creneauMisAJour(d);
        return toDto(saved);
    }

    // ------------------------------------------------------------------
    // Règles communes
    // ------------------------------------------------------------------

    private Utilisateur clientConnecte() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur utilisateur = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable"));
        if (!ClientAccounts.isClient(utilisateur)) {
            throw new ForbiddenException("Seul un compte client peut réserver un rendez-vous");
        }
        return utilisateur;
    }

    /** Rendez-vous dont l'appelant est le client ou le praticien ; sinon 403. */
    private RendezVous rendezVousDuParticipant(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        RendezVous rdv = rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable"));

        boolean isClient = rdv.getPatient() != null && rdv.getPatient().getId().equals(uid);
        boolean isPro = rdv.getProfessionnel() != null && rdv.getProfessionnel().getId().equals(uid);
        if (!isClient && !isPro) {
            throw new ForbiddenException("Vous n'avez pas accès à ce rendez-vous");
        }
        return rdv;
    }

    private void verifierProfessionnelValide(Professionnel pro) {
        if (!AccesProfessionnel.estApprouve(pro)) {
            throw new BadRequestException("Ce professionnel n'est pas disponible à la réservation sur la plateforme");
        }
    }

    /**
     * Le montant d'une réservation directe doit correspondre à un tarif publié et actif
     * du professionnel : le client ne choisit jamais librement le prix.
     */
    private BigDecimal verifierTarif(Professionnel pro, BigDecimal montantDemande) {
        List<TarifProfessionnel> tarifs = pro.getTarifs() == null ? List.of() : pro.getTarifs().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActif()) && t.getMontant() != null)
                .toList();
        if (tarifs.isEmpty()) {
            throw new BadRequestException("Ce professionnel n'a pas encore publié de tarif : réservation impossible pour le moment.");
        }
        return tarifs.stream()
                .map(TarifProfessionnel::getMontant)
                .filter(montant -> montantDemande != null && montant.compareTo(montantDemande) == 0)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Le montant ne correspond à aucun tarif publié par ce professionnel."));
    }

    /** Créneau relu sous verrou et vérifié : libre, futur et appartenant au professionnel. */
    /**
     * Un client ne peut pas avoir deux rendez-vous actifs qui se chevauchent, avec le même
     * praticien ou un autre ({@code rdvIgnore} : rendez-vous en cours de déplacement).
     */
    private void verifierClientDisponible(Utilisateur client, Disponibilite creneau, String rdvIgnore) {
        if (client == null) {
            return;
        }
        LocalDateTime debut = debutDe(creneau);
        LocalDateTime fin = debut.plusMinutes(dureeDe(creneau));
        // Un rendez-vous commencé jusqu'à 12 h avant peut encore chevaucher le créneau.
        for (RendezVous r : rendezVousRepository.findByClientIdAndStatutInAndDateHeureBetween(
                client.getId(), STATUTS_MODIFIABLES, debut.minusHours(12), fin)) {
            if (r.getId().equals(rdvIgnore)) {
                continue;
            }
            LocalDateTime finR = r.getDateHeure().plusMinutes(r.getDureeMinutes() != null ? r.getDureeMinutes() : 45);
            if (r.getDateHeure().isBefore(fin) && finR.isAfter(debut)) {
                boolean memePraticien = r.getProfessionnel() != null && creneau.getProfessionnel() != null
                        && r.getProfessionnel().getId().equals(creneau.getProfessionnel().getId());
                throw new ConflictException(memePraticien
                        ? "Vous avez déjà un rendez-vous avec ce professionnel à ce moment-là."
                        : "Vous avez déjà un rendez-vous à ce moment-là. Choisissez un autre créneau.");
            }
        }
    }

    private Disponibilite creneauLibreVerrouille(String disponibiliteId, Professionnel pro) {
        Disponibilite disp = disponibiliteRepository.findByIdForUpdate(disponibiliteId)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau de disponibilité introuvable"));

        if (!CRENEAU_LIBRE.equalsIgnoreCase(disp.getStatut())) {
            throw new BadRequestException("Ce créneau n'est plus disponible");
        }
        if (pro == null || disp.getProfessionnel() == null || !disp.getProfessionnel().getId().equals(pro.getId())) {
            throw new BadRequestException("Le créneau n'appartient pas au professionnel sélectionné");
        }
        if (debutDe(disp).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Ce créneau est expiré et ne peut plus être réservé.");
        }
        return disp;
    }

    private String modeOuDefaut(String mode, String parDefaut) {
        if (mode == null || mode.isBlank()) {
            return parDefaut;
        }
        String normalise = mode.trim().toUpperCase();
        if (!MODES_AUTORISES.contains(normalise)) {
            throw new BadRequestException("Mode de consultation invalide (VISIO ou CABINET)");
        }
        return normalise;
    }

    private LocalDateTime debutDe(Disponibilite d) {
        return LocalDateTime.of(d.getDate(), d.getHeureDebut());
    }

    private int dureeDe(Disponibilite d) {
        if (d.getHeureFin() == null) {
            return DUREE_PAR_DEFAUT_MINUTES;
        }
        return (int) Duration.between(d.getHeureDebut(), d.getHeureFin()).toMinutes();
    }

    private LocalDateTime finDe(RendezVous r) {
        if (r.getDateHeure() == null) {
            return null;
        }
        int duree = r.getDureeMinutes() != null ? r.getDureeMinutes() : DUREE_PAR_DEFAUT_MINUTES;
        return r.getDateHeure().plusMinutes(duree);
    }

    private boolean doitGenererLienVisio(String lienActuel) {
        return lienActuel == null || lienActuel.isBlank() || lienActuel.contains("meet.google.com") || lienActuel.contains("meet.jit.si");
    }

    /**
     * Génère un lien de salle de visioconférence WebRTC Jitsi Meet fonctionnelle et intégrable
     * directement dans PsyAvocat ({@code https://meet.jit.si/PsyAvocat-Consultation-xxx-xxxx-xxx}).
     */
    private String genererLienVisioPsyAvocat(String seed) {
        String source = (seed != null && !seed.isBlank()) ? seed : java.util.UUID.randomUUID().toString();
        long hash = 1125899906842597L;
        for (int i = 0; i < source.length(); i++) {
            hash = 31L * hash + source.charAt(i);
        }
        hash = Math.abs(hash);
        char[] chars = new char[10];
        for (int i = 0; i < 10; i++) {
            chars[i] = (char) ('a' + (int) ((hash + i * 7L) % 26));
            hash /= 26;
            if (hash == 0) {
                hash = Math.abs((long) source.hashCode() + (i + 1) * 9973L);
            }
        }
        return "https://meet.ffmuc.net/PsyAvocat-Consultation-"
                + new String(chars, 0, 3) + "-"
                + new String(chars, 3, 4) + "-"
                + new String(chars, 7, 3);
    }

    private RendezVousResponseDTO toDto(RendezVous rdv) {
        LocalDateTime fin = finDe(rdv);
        boolean estPasse = (fin != null && fin.isBefore(LocalDateTime.now()))
                || "PASSE".equalsIgnoreCase(rdv.getStatut())
                || "ANNULE".equalsIgnoreCase(rdv.getStatut());
        String lienVisioActif = estPasse ? null : rdv.getLienVisio();

        RendezVousResponseDTO.RendezVousResponseDTOBuilder builder = RendezVousResponseDTO.builder()
                .id(rdv.getId())
                .dateHeure(rdv.getDateHeure())
                .dateFin(fin)
                .dureeMinutes(rdv.getDureeMinutes())
                .statut(rdv.getStatut())
                .mode(rdv.getMode())
                .motif(rdv.getMotif())
                .devise(rdv.getDevise())
                .lienVisio(lienVisioActif)
                .adresseCabinet(rdv.getAdresseCabinet())
                .montantTotal(rdv.getMontantTotal())
                .disponibiliteId(rdv.getDisponibilite() != null ? rdv.getDisponibilite().getId() : null);

        if (rdv.getPatient() != null) {
            builder.patientId(rdv.getPatient().getId())
                    .patientNom(rdv.getPatient().getNom())
                    .patientPrenom(rdv.getPatient().getPrenom());
        }

        Professionnel pro = rdv.getProfessionnel();
        if (pro != null) {
            builder.professionnelId(pro.getId())
                    .professionnelNom(pro.getNom())
                    .professionnelPrenom(pro.getPrenom())
                    .typeProfessionnel(Entites.reel(pro) instanceof Avocat ? "AVOCAT" : "PSYCHOLOGUE")
                    .professionnelPhotoUrl(mediaUrlResolver.photoUrlOf(pro));
            if (pro.getSpecialites() != null && !pro.getSpecialites().isEmpty()) {
                builder.professionnelSpecialite(pro.getSpecialites().get(0).getNom());
            }
        }

        SoumissionDossier soumission = rdv.getSoumission();
        if (soumission != null) {
            builder.soumissionId(soumission.getId());
            if (soumission.getDossier() != null) {
                builder.dossierId(soumission.getDossier().getId())
                        .dossierTitre(soumission.getDossier().getTitre());
            }
        }

        if (rdv.getPaiement() != null) {
            builder.montantAcompte(rdv.getPaiement().getMontant())
                    .statutPaiement(rdv.getPaiement().getStatut());
        } else if (rdv.getMontantAcompte() != null) {
            builder.montantAcompte(rdv.getMontantAcompte());
        }

        return builder.build();
    }
}
