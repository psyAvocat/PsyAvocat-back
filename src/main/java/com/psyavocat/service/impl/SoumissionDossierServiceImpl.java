package com.psyavocat.service.impl;

import com.psyavocat.dto.dossier.RepondreSoumissionRequest;
import com.psyavocat.dto.dossier.SoumissionDossierRequest;
import com.psyavocat.dto.dossier.SoumissionDossierResponseDTO;
import com.psyavocat.entity.Avocat;
import com.psyavocat.entity.Dossier;
import com.psyavocat.entity.SoumissionDossier;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.AvocatRepository;
import com.psyavocat.repository.DossierRepository;
import com.psyavocat.repository.SoumissionDossierRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.SoumissionDossierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SoumissionDossierServiceImpl implements SoumissionDossierService {

    private final DossierRepository dossierRepository;
    private final SoumissionDossierRepository soumissionDossierRepository;
    private final AvocatRepository avocatRepository;
    private final AuthenticationContext authenticationContext;

    public SoumissionDossierServiceImpl(
            DossierRepository dossierRepository,
            SoumissionDossierRepository soumissionDossierRepository,
            AvocatRepository avocatRepository,
            AuthenticationContext authenticationContext
    ) {
        this.dossierRepository = dossierRepository;
        this.soumissionDossierRepository = soumissionDossierRepository;
        this.avocatRepository = avocatRepository;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public SoumissionDossierResponseDTO soumettreDossier(String dossierId, SoumissionDossierRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        if (!dossier.getJusticiable().getId().equals(uid)) {
            throw new ForbiddenException("Vous devez être le propriétaire du dossier pour le soumettre");
        }

        Avocat avocat = avocatRepository.findById(request.getAvocatId())
                .orElseThrow(() -> new ResourceNotFoundException("Avocat introuvable"));

        if (!"APPROVED".equalsIgnoreCase(avocat.getStatutValidation())) {
            throw new BadRequestException("Cet avocat n'est pas encore validé par la plateforme");
        }

        boolean dejaSoumis = dossier.getSoumissions() != null && dossier.getSoumissions().stream()
                .anyMatch(s -> s.getAvocat() != null && s.getAvocat().getId().equals(avocat.getId()));
        if (dejaSoumis) {
            throw new ConflictException("Ce dossier a déjà été soumis à cet avocat");
        }

        SoumissionDossier soumission = new SoumissionDossier();
        soumission.setDossier(dossier);
        soumission.setAvocat(avocat);
        soumission.setDateSoumission(LocalDateTime.now());
        soumission.setStatut("EN_ATTENTE");

        SoumissionDossier saved = soumissionDossierRepository.save(soumission);
        return toSoumissionDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoumissionDossierResponseDTO> getSoumissionsPourAvocat(String statut) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        List<SoumissionDossier> list;
        if (statut != null && !statut.isBlank()) {
            list = soumissionDossierRepository.findByAvocatIdAndStatutOrderByDateSoumissionDesc(uid, statut);
        } else {
            // Par défaut, ne liste que les soumissions actives (exclut les dossiers devenus CADUQUE car pris par un autre confrère)
            list = soumissionDossierRepository.findByAvocatIdOrderByDateSoumissionDesc(uid).stream()
                    .filter(s -> !"CADUQUE".equalsIgnoreCase(s.getStatut()))
                    .toList();
        }
        return list.stream().map(this::toSoumissionDto).toList();
    }

    @Override
    public SoumissionDossierResponseDTO repondreSoumission(String soumissionId, RepondreSoumissionRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        SoumissionDossier soumission = soumissionDossierRepository.findById(soumissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Soumission introuvable"));

        if (!soumission.getAvocat().getId().equals(uid)) {
            throw new ForbiddenException("Vous n'êtes pas l'avocat destinataire de cette soumission");
        }

        if ("CADUQUE".equalsIgnoreCase(soumission.getStatut()) ||
            (soumission.getDossier() != null && "PRIS_EN_CHARGE".equalsIgnoreCase(soumission.getDossier().getStatut()))) {
            throw new BadRequestException("Ce dossier juridique a déjà été confié et pris en charge par un autre confrère");
        }

        String statut = request.getStatut().toUpperCase();
        if (!"ACCEPTEE".equals(statut) && !"REFUSEE".equals(statut)) {
            throw new BadRequestException("Statut invalide. Valeurs possibles : ACCEPTEE, REFUSEE");
        }

        if ("ACCEPTEE".equals(statut)) {
            if (request.getTarifPropose() == null || request.getTarifPropose().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Un tarif proposé supérieur à 0 est requis en cas d'acceptation du dossier");
            }
            soumission.setTarifPropose(request.getTarifPropose());
        }

        soumission.setStatut(statut);
        soumission.setReponse(request.getReponse());
        soumission.setDateReponse(LocalDateTime.now());

        SoumissionDossier saved = soumissionDossierRepository.save(soumission);
        return toSoumissionDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoumissionDossierResponseDTO> getSoumissionsParDossier(String dossierId) {
        return soumissionDossierRepository.findByDossierId(dossierId)
                .stream()
                .map(this::toSoumissionDto)
                .toList();
    }

    public SoumissionDossierResponseDTO toSoumissionDto(SoumissionDossier s) {
        SoumissionDossierResponseDTO.SoumissionDossierResponseDTOBuilder builder = SoumissionDossierResponseDTO.builder()
                .id(s.getId())
                .dateSoumission(s.getDateSoumission())
                .statut(s.getStatut())
                .reponse(s.getReponse())
                .tarifPropose(s.getTarifPropose())
                .dateReponse(s.getDateReponse());

        if (s.getDossier() != null) {
            builder.dossierId(s.getDossier().getId())
                    .dossierTitre(s.getDossier().getTitre())
                    .dossierDescription(s.getDossier().getDescription());

            if (s.getDossier().getJusticiable() != null) {
                builder.justiciableId(s.getDossier().getJusticiable().getId())
                        .justiciableNom(s.getDossier().getJusticiable().getNom())
                        .justiciablePrenom(s.getDossier().getJusticiable().getPrenom());
            }
        }

        if (s.getAvocat() != null) {
            builder.avocatId(s.getAvocat().getId())
                    .avocatNom(s.getAvocat().getNom())
                    .avocatPrenom(s.getAvocat().getPrenom());
        }

        return builder.build();
    }
}
