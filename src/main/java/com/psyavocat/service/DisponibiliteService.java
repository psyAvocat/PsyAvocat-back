package com.psyavocat.service;

import com.psyavocat.dto.disponibilite.CreateDisponibiliteRequest;
import com.psyavocat.dto.disponibilite.DisponibiliteResponseDTO;
import com.psyavocat.entity.Disponibilite;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.DisponibiliteRepository;
import com.psyavocat.repository.ProfessionnelRepository;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DisponibiliteService {

    private final DisponibiliteRepository disponibiliteRepository;
    private final ProfessionnelRepository professionnelRepository;
    private final AuthenticationContext authenticationContext;

    public DisponibiliteService(
            DisponibiliteRepository disponibiliteRepository,
            ProfessionnelRepository professionnelRepository,
            AuthenticationContext authenticationContext
    ) {
        this.disponibiliteRepository = disponibiliteRepository;
        this.professionnelRepository = professionnelRepository;
        this.authenticationContext = authenticationContext;
    }

    public DisponibiliteResponseDTO createDisponibilite(CreateDisponibiliteRequest request) {
        if (!request.getHeureDebut().isBefore(request.getHeureFin())) {
            throw new BadRequestException("L'heure de début doit être antérieure à l'heure de fin");
        }

        String uid = authenticationContext.getRequiredFirebaseUid();
        Professionnel pro = professionnelRepository.findById(uid)
                .orElseThrow(() -> new ForbiddenException("Seul un professionnel peut définir ses disponibilités"));

        Disponibilite disponibilite = new Disponibilite();
        disponibilite.setDate(request.getDate());
        disponibilite.setHeureDebut(request.getHeureDebut());
        disponibilite.setHeureFin(request.getHeureFin());
        disponibilite.setStatut("LIBRE");
        disponibilite.setProfessionnel(pro);

        Disponibilite saved = disponibiliteRepository.save(disponibilite);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DisponibiliteResponseDTO> getMyDisponibilites() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return disponibiliteRepository.findByProfessionnelId(uid).stream()
                .map(this::toDto)
                .toList();
    }

    public void deleteDisponibilite(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Disponibilite disp = disponibiliteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilité introuvable"));

        if (!disp.getProfessionnel().getId().equals(uid)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à supprimer cette disponibilité");
        }

        if (!"LIBRE".equalsIgnoreCase(disp.getStatut())) {
            throw new BadRequestException("Impossible de supprimer une disponibilité déjà réservée");
        }

        disponibiliteRepository.delete(disp);
    }

    @Transactional(readOnly = true)
    public List<DisponibiliteResponseDTO> getDisponibilitesLibres(String professionnelId) {
        return disponibiliteRepository.findByProfessionnelIdAndDateGreaterThanEqualOrderByDateAscHeureDebutAsc(
                professionnelId,
                LocalDate.now()
        ).stream()
                .filter(d -> "LIBRE".equalsIgnoreCase(d.getStatut()))
                .map(this::toDto)
                .toList();
    }

    private DisponibiliteResponseDTO toDto(Disponibilite d) {
        return DisponibiliteResponseDTO.builder()
                .id(d.getId())
                .date(d.getDate())
                .heureDebut(d.getHeureDebut())
                .heureFin(d.getHeureFin())
                .statut(d.getStatut())
                .professionnelId(d.getProfessionnel() != null ? d.getProfessionnel().getId() : null)
                .build();
    }
}
