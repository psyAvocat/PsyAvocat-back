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

    public ProfessionnelServiceImpl(
            ProfessionnelRepository professionnelRepository,
            ProfessionnelMapper professionnelMapper
    ) {
        this.professionnelRepository = professionnelRepository;
        this.professionnelMapper = professionnelMapper;
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
        return professionnelRepository.findByStatutValidation("PENDING").stream()
                .map(professionnelMapper::toDto)
                .toList();
    }

    @Override
    public ProfessionnelResponseDTO updateStatutValidation(String id, String nouveauStatut) {
        if (!STATUTS_VALIDES.contains(nouveauStatut)) {
            throw new BadRequestException("Statut invalide : " + nouveauStatut + ". Valeurs acceptées : " + STATUTS_VALIDES);
        }

        Professionnel pro = professionnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professionnel introuvable avec l'identifiant : " + id));

        pro.setStatutValidation(nouveauStatut);
        Professionnel saved = professionnelRepository.save(pro);
        return professionnelMapper.toDto(saved);
    }
}
