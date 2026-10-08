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

    public ProfessionnelServiceImpl(
            ProfessionnelRepository professionnelRepository,
            ProfessionnelMapper professionnelMapper,
            com.psyavocat.service.NotificationService notificationService,
            com.psyavocat.realtime.RealtimeGateway realtimeGateway
    ) {
        this.professionnelRepository = professionnelRepository;
        this.professionnelMapper = professionnelMapper;
        this.notificationService = notificationService;
        this.realtimeGateway = realtimeGateway;
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
    public ProfessionnelResponseDTO updateStatutValidation(String id, String nouveauStatut) {
        return updateStatutValidation(id, nouveauStatut, null);
    }

    @Override
    public ProfessionnelResponseDTO updateStatutValidation(String id, String nouveauStatut, String motif) {
        if (!STATUTS_VALIDES.contains(nouveauStatut)) {
            throw new BadRequestException("Statut invalide : " + nouveauStatut + ". Valeurs acceptées : " + STATUTS_VALIDES);
        }

        Professionnel pro = professionnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professionnel introuvable avec l'identifiant : " + id));

        if ("REJECTED".equalsIgnoreCase(nouveauStatut)) {
            if (motif == null || motif.trim().isEmpty()) {
                throw new BadRequestException("Le motif est obligatoire pour refuser un dossier professionnel.");
            }
            pro.setMotifRefus(motif.trim());
            notificationService.sendNotification(
                    pro.getId(),
                    "VALIDATION_REFUSEE",
                    "Votre dossier d'inscription professionnelle a été refusé. Motif : " + motif.trim(),
                    null
            );
        } else if ("APPROVED".equalsIgnoreCase(nouveauStatut)) {
            pro.setMotifRefus(null);
            notificationService.sendNotification(
                    pro.getId(),
                    "VALIDATION_APPROUVEE",
                    "Félicitations ! Votre profil professionnel a été validé par l'administration. Vous avez désormais un accès complet à l'espace professionnel.",
                    null
            );
        } else if ("SUSPENDED".equalsIgnoreCase(nouveauStatut)) {
            notificationService.sendNotification(
                    pro.getId(),
                    "COMPTE_SUSPENDU",
                    "Votre compte professionnel a été suspendu par l'administration." + (motif != null && !motif.isBlank() ? " Motif : " + motif.trim() : ""),
                    null
            );
        }

        pro.setStatutValidation(nouveauStatut);
        Professionnel saved = professionnelRepository.save(pro);
        // Les listes des clients connectés se mettent à jour (apparition / retrait du professionnel).
        realtimeGateway.diffuser("PROFESSIONNEL_MIS_A_JOUR", java.util.Map.of("id", saved.getId(), "statut", nouveauStatut));
        return professionnelMapper.toDto(saved);
    }
}

