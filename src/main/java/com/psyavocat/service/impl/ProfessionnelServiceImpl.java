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

    public ProfessionnelServiceImpl(
            ProfessionnelRepository professionnelRepository,
            ProfessionnelMapper professionnelMapper,
            com.psyavocat.service.NotificationService notificationService
    ) {
        this.professionnelRepository = professionnelRepository;
        this.professionnelMapper = professionnelMapper;
        this.notificationService = notificationService;
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
        return professionnelMapper.toDto(saved);
    }
}

