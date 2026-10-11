package com.psyavocat.service.impl;

import com.psyavocat.dto.professionnel.ProfessionnelResponseDTO;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.Psychologue;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.mapper.ProfessionnelMapper;
import com.psyavocat.repository.ProfessionnelRepository;
import com.psyavocat.service.ProfessionnelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProfessionnelServiceImpl implements ProfessionnelService {

    private static final Set<String> STATUTS_VALIDES = Set.of("PENDING", "APPROVED", "REJECTED", "SUSPENDED");

    private final ProfessionnelRepository professionnelRepository;
    private final ProfessionnelMapper professionnelMapper;
    private final com.psyavocat.service.NotificationService notificationService;
    private final com.psyavocat.realtime.RealtimeGateway realtimeGateway;
    private final com.psyavocat.repository.RendezVousRepository rendezVousRepository;
    private final com.psyavocat.repository.DisponibiliteRepository disponibiliteRepository;
    private final com.psyavocat.service.notification.RendezVousEvenements rendezVousEvenements;
    private final com.psyavocat.service.support.JournalDecisions journalDecisions;

    public ProfessionnelServiceImpl(
            ProfessionnelRepository professionnelRepository,
            ProfessionnelMapper professionnelMapper,
            com.psyavocat.service.NotificationService notificationService,
            com.psyavocat.realtime.RealtimeGateway realtimeGateway,
            com.psyavocat.repository.RendezVousRepository rendezVousRepository,
            com.psyavocat.repository.DisponibiliteRepository disponibiliteRepository,
            com.psyavocat.service.notification.RendezVousEvenements rendezVousEvenements,
            com.psyavocat.service.support.JournalDecisions journalDecisions
    ) {
        this.professionnelRepository = professionnelRepository;
        this.professionnelMapper = professionnelMapper;
        this.notificationService = notificationService;
        this.realtimeGateway = realtimeGateway;
        this.rendezVousRepository = rendezVousRepository;
        this.disponibiliteRepository = disponibiliteRepository;
        this.rendezVousEvenements = rendezVousEvenements;
        this.journalDecisions = journalDecisions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionnelResponseDTO> rechercherPublic(String type, String q, String ville,
                                                           String specialiteId, String modeConsultation) {
        String texte = q != null && !q.isBlank() ? normaliser(q) : null;
        return professionnelRepository.searchProfessionnels("APPROVED", ville, modeConsultation, specialiteId).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActif()))
                .filter(p -> type == null || type.isBlank()
                        || ("AVOCAT".equalsIgnoreCase(type) && p instanceof Avocat)
                        || ("PSYCHOLOGUE".equalsIgnoreCase(type) && p instanceof Psychologue))
                .filter(p -> texte == null || correspond(p, texte))
                .map(professionnelMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfessionnelResponseDTO getProfessionnelPublic(String id) {
        Professionnel pro = professionnelRepository.findById(id)
                .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatutValidation()))
                .filter(p -> !Boolean.FALSE.equals(p.getActif()))
                .orElseThrow(() -> new ResourceNotFoundException("Cette ressource n'est plus disponible."));
        return professionnelMapper.toDto(pro);
    }

    /** Recherche texte : nom, prénom, ville ou nom d'une spécialité. */
    private boolean correspond(Professionnel p, String texte) {
        if (contient(p.getNom(), texte) || contient(p.getPrenom(), texte) || contient(p.getVille(), texte)
                || contient(p.getPrenom() + " " + p.getNom(), texte)) {
            return true;
        }
        return p.getSpecialites() != null && p.getSpecialites().stream().anyMatch(s -> contient(s.getNom(), texte));
    }

    private boolean contient(String valeur, String texte) {
        return valeur != null && normaliser(valeur).contains(texte);
    }

    /** Minuscules sans accents : « Médiation » trouve « mediation ». */
    private String normaliser(String valeur) {
        return java.text.Normalizer.normalize(valeur.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionnelResponseDTO> searchProfessionnels(
            String type,
            String ville,
            String specialiteId,
            String modeConsultation
    ) {
        List<Professionnel> list = professionnelRepository.searchProfessionnels(
                "APPROVED",
                ville,
                modeConsultation,
                specialiteId
        );

        if ("AVOCAT".equalsIgnoreCase(type)) {
            list = list.stream().filter(p -> p instanceof Avocat).toList();
        } else if ("PSYCHOLOGUE".equalsIgnoreCase(type)) {
            list = list.stream().filter(p -> p instanceof Psychologue).toList();
        }

        return list.stream().map(professionnelMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfessionnelResponseDTO getProfessionnelById(String id) {
        Professionnel pro = professionnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professionnel introuvable avec l'identifiant : " + id));
        return professionnelMapper.toDto(pro);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionnelResponseDTO> getProfessionnelsEnAttente() {
        return professionnelRepository.findEnAttente().stream()
                .map(professionnelMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionnelResponseDTO> getProfessionnelsAdmin(String statut) {
        String filtre = statut != null && !statut.isBlank() ? statut.trim().toUpperCase() : null;
        if (filtre != null && !STATUTS_VALIDES.contains(filtre)) {
            throw new BadRequestException("Statut invalide : " + statut + ". Valeurs acceptées : " + STATUTS_VALIDES);
        }
        List<Professionnel> list = filtre == null
                ? professionnelRepository.findAll()
                : professionnelRepository.findByStatutValidation(filtre);
        return list.stream()
                .sorted(java.util.Comparator.comparing(Professionnel::getDateInscription,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(professionnelMapper::toDto)
                .toList();
    }

    @Override
    public ProfessionnelResponseDTO updateStatutValidation(String id, String nouveauStatut) {
        return updateStatutValidation(id, nouveauStatut, null);
    }

    /**
     * Décision administrative sur un professionnel.
     *
     * Règles :
     * - REJECTED et SUSPENDED exigent un motif, transmis au professionnel ;
     * - l'accès aux fonctions métier est recalculé immédiatement (contrôle serveur à chaque requête) ;
     * - REJECTED / SUSPENDED : les rendez-vous FUTURS confirmés sont annulés (créneau libéré, acompte
     *   simulé annulé) et les clients concernés sont notifiés ; les créneaux et la fiche disparaissent
     *   des listes publiques (réservés aux professionnels approuvés) ;
     * - la décision est historisée et diffusée en temps réel (listes clients, session du professionnel).
     */
    @Override
    public ProfessionnelResponseDTO updateStatutValidation(String id, String nouveauStatut, String motif) {
        String statut = nouveauStatut != null ? nouveauStatut.trim().toUpperCase() : "";
        if (!STATUTS_VALIDES.contains(statut)) {
            throw new BadRequestException("Statut invalide : " + nouveauStatut + ". Valeurs acceptées : " + STATUTS_VALIDES);
        }
        String motifNettoye = motif != null && !motif.isBlank() ? motif.trim() : null;
        if (("REJECTED".equals(statut) || "SUSPENDED".equals(statut)) && motifNettoye == null) {
            throw new BadRequestException("Le motif est obligatoire pour refuser ou suspendre un compte professionnel.");
        }

        Professionnel pro = professionnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professionnel introuvable avec l'identifiant : " + id));

        switch (statut) {
            case "REJECTED" -> {
                pro.setMotifRefus(motifNettoye);
                notificationService.sendNotification(pro.getId(), "VALIDATION_REFUSEE",
                        "Votre dossier d'inscription professionnelle a été refusé. Motif : " + motifNettoye, null);
            }
            case "APPROVED" -> {
                pro.setMotifRefus(null);
                notificationService.sendNotification(pro.getId(), "VALIDATION_APPROUVEE",
                        "Félicitations ! Votre profil professionnel a été validé par l'administration. Vous avez désormais un accès complet à l'espace professionnel.",
                        null);
            }
            case "SUSPENDED" -> notificationService.sendNotification(pro.getId(), "COMPTE_SUSPENDU",
                    "Votre compte professionnel a été suspendu par l'administration. Motif : " + motifNettoye, null);
            default -> {
                // PENDING : remise en vérification, sans notification spécifique.
            }
        }

        pro.setStatutValidation(statut);
        Professionnel saved = professionnelRepository.save(pro);

        if ("REJECTED".equals(statut) || "SUSPENDED".equals(statut)) {
            annulerRendezVousFuturs(saved);
        }

        journalDecisions.enregistrer(com.psyavocat.service.support.JournalDecisions.TYPE_STATUT_PROFESSIONNEL,
                saved.getId(), statut, motifNettoye);

        // Les listes des clients connectés se mettent à jour (apparition / retrait du professionnel)
        // et l'espace du professionnel relit immédiatement ses droits.
        realtimeGateway.diffuser("PROFESSIONNEL_MIS_A_JOUR", java.util.Map.of("id", saved.getId(), "statut", statut));
        realtimeGateway.envoyerA(saved.getId(), "COMPTE_MIS_A_JOUR", java.util.Map.of("statutValidation", statut));
        return professionnelMapper.toDto(saved);
    }

    /** Rendez-vous futurs du professionnel annulés : créneaux libérés, acomptes simulés annulés, clients prévenus. */
    private void annulerRendezVousFuturs(Professionnel pro) {
        java.time.LocalDateTime maintenant = java.time.LocalDateTime.now();
        for (com.psyavocat.entity.RendezVous rdv : rendezVousRepository.findByProfessionnelIdOrderByDateHeureDesc(pro.getId())) {
            boolean actif = "CONFIRME".equalsIgnoreCase(rdv.getStatut()) || "EN_ATTENTE".equalsIgnoreCase(rdv.getStatut());
            if (!actif || rdv.getDateHeure() == null || !rdv.getDateHeure().isAfter(maintenant)) {
                continue;
            }
            rdv.setStatut("ANNULE");
            if (rdv.getPaiement() != null && "PAYE".equalsIgnoreCase(rdv.getPaiement().getStatut())) {
                rdv.getPaiement().setStatut("ANNULE");
            }
            com.psyavocat.entity.Disponibilite creneau = rdv.getDisponibilite();
            if (creneau != null) {
                creneau.setStatut("LIBRE");
                disponibiliteRepository.save(creneau);
            }
            rendezVousRepository.save(rdv);
            rendezVousEvenements.rendezVousAnnuleParPlateforme(rdv);
            rendezVousEvenements.creneauMisAJour(creneau);
        }
    }
}
