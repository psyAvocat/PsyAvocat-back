package com.psyavocat.service.impl;

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
import com.psyavocat.service.DisponibiliteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DisponibiliteServiceImpl implements DisponibiliteService {

    private final DisponibiliteRepository disponibiliteRepository;
    private final ProfessionnelRepository professionnelRepository;
    private final AuthenticationContext authenticationContext;

    public DisponibiliteServiceImpl(
            DisponibiliteRepository disponibiliteRepository,
            ProfessionnelRepository professionnelRepository,
            AuthenticationContext authenticationContext
    ) {
        this.disponibiliteRepository = disponibiliteRepository;
        this.professionnelRepository = professionnelRepository;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public DisponibiliteResponseDTO createDisponibilite(CreateDisponibiliteRequest request) {
        if (request.getHeureDebut() == null || request.getHeureFin() == null) {
            throw new BadRequestException("L'heure de début et l'heure de fin sont obligatoires");
        }

        if (!request.getHeureDebut().isBefore(request.getHeureFin())) {
            throw new BadRequestException("L'heure de début (" + request.getHeureDebut() + ") doit être antérieure à l'heure de fin (" + request.getHeureFin() + ")");
        }

        LocalDate today = LocalDate.now();
        if (request.getDate().isBefore(today)) {
            throw new BadRequestException("Impossible de créer un créneau pour une date déjà passée (" + request.getDate() + ")");
        }
        if (request.getDate().isEqual(today) && request.getHeureFin().isBefore(java.time.LocalTime.now())) {
            throw new BadRequestException("Impossible de créer un créneau pour une heure déjà passée aujourd'hui");
        }

        String uid = authenticationContext.getRequiredFirebaseUid();
        Professionnel pro = professionnelRepository.findById(uid)
                .orElseThrow(() -> new ForbiddenException("Seul un professionnel peut définir ses disponibilités"));

        // Vérification stricte de non-chevauchement et d'unicité d'heure pour ce professionnel ce jour-là
        List<Disponibilite> conflits = disponibiliteRepository.findConflictingDisponibilites(
                pro.getId(),
                request.getDate(),
                request.getHeureDebut(),
                request.getHeureFin()
        );

        if (!conflits.isEmpty()) {
            Disponibilite conflit = conflits.get(0);
            if (conflit.getHeureDebut().equals(request.getHeureDebut())) {
                throw new BadRequestException(
                        "Vous avez déjà un créneau débutant à " + request.getHeureDebut() + " le " + request.getDate() + ". Impossible d'ajouter un doublon."
                );
            } else {
                throw new BadRequestException(
                        "Ce créneau (" + request.getHeureDebut() + " - " + request.getHeureFin() + ") chevauche un créneau déjà existant ("
                                + conflit.getHeureDebut() + " - " + conflit.getHeureFin() + ") pour le " + request.getDate() + "."
                );
            }
        }

        Disponibilite disponibilite = new Disponibilite();
        disponibilite.setDate(request.getDate());
        disponibilite.setHeureDebut(request.getHeureDebut());
        disponibilite.setHeureFin(request.getHeureFin());
        disponibilite.setStatut("LIBRE");
        disponibilite.setProfessionnel(pro);

        Disponibilite saved = disponibiliteRepository.save(disponibilite);
        return toDto(saved);
    }

    @Override
    public List<DisponibiliteResponseDTO> createDisponibilitesBatch(List<CreateDisponibiliteRequest> requests) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Professionnel pro = professionnelRepository.findById(uid)
                .orElseThrow(() -> new ForbiddenException("Seul un professionnel peut définir ses disponibilités"));

        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();

        // Récupérer les créneaux déjà existants pour ce professionnel à partir d'aujourd'hui
        List<Disponibilite> existantes = disponibiliteRepository.findByProfessionnelIdAndDateGreaterThanEqualOrderByDateAscHeureDebutAsc(
                pro.getId(),
                today
        );

        List<Disponibilite> toSave = new java.util.ArrayList<>();

        for (CreateDisponibiliteRequest req : requests) {
            if (req.getDate() == null || req.getHeureDebut() == null || req.getHeureFin() == null) {
                continue;
            }
            if (!req.getHeureDebut().isBefore(req.getHeureFin())) {
                continue;
            }
            if (req.getDate().isBefore(today)) {
                continue;
            }
            if (req.getDate().isEqual(today) && req.getHeureFin().isBefore(now)) {
                continue;
            }

            // Vérifier conflit avec les disponibilités existantes en base
            boolean conflitExistant = existantes.stream().anyMatch(e ->
                    e.getDate().isEqual(req.getDate())
                            && req.getHeureDebut().isBefore(e.getHeureFin())
                            && req.getHeureFin().isAfter(e.getHeureDebut())
            );
            if (conflitExistant) {
                continue; // Créneau ignoré : déjà existant ou en conflit
            }

            // Vérifier conflit avec les créneaux déjà accumulés dans ce même lot
            boolean conflitLot = toSave.stream().anyMatch(s ->
                    s.getDate().isEqual(req.getDate())
                            && req.getHeureDebut().isBefore(s.getHeureFin())
                            && req.getHeureFin().isAfter(s.getHeureDebut())
            );
            if (conflitLot) {
                continue;
            }

            Disponibilite d = new Disponibilite();
            d.setDate(req.getDate());
            d.setHeureDebut(req.getHeureDebut());
            d.setHeureFin(req.getHeureFin());
            d.setStatut("LIBRE");
            d.setProfessionnel(pro);
            toSave.add(d);
        }

        if (toSave.isEmpty() && !requests.isEmpty()) {
            throw new BadRequestException("Aucun nouveau créneau à ajouter : tous les créneaux demandés existent déjà ou sont en conflit d'horaires.");
        }

        return disponibiliteRepository.saveAll(toSave).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibiliteResponseDTO> getMyDisponibilites() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return disponibiliteRepository.findByProfessionnelId(uid).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public List<DisponibiliteResponseDTO> getDisponibilitesLibres(String professionnelId) {
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();

        return disponibiliteRepository.findByProfessionnelIdAndDateGreaterThanEqualOrderByDateAscHeureDebutAsc(
                professionnelId,
                today
        ).stream()
                .filter(d -> "LIBRE".equalsIgnoreCase(d.getStatut()))
                .filter(d -> {
                    // Si le créneau est aujourd'hui, masquer s'il est déjà entamé ou passé
                    if (d.getDate().isEqual(today)) {
                        return d.getHeureDebut().isAfter(now);
                    }
                    // Si le créneau est dans le futur
                    return d.getDate().isAfter(today);
                })
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
