package com.psyavocat.service;

import com.psyavocat.dto.dossier.*;
import com.psyavocat.entity.*;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.*;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class DossierService {

    private final DossierRepository dossierRepository;
    private final SoumissionDossierRepository soumissionDossierRepository;
    private final JusticiableRepository justiciableRepository;
    private final AvocatRepository avocatRepository;
    private final AuthenticationContext authenticationContext;

    public DossierService(
            DossierRepository dossierRepository,
            SoumissionDossierRepository soumissionDossierRepository,
            JusticiableRepository justiciableRepository,
            AvocatRepository avocatRepository,
            AuthenticationContext authenticationContext
    ) {
        this.dossierRepository = dossierRepository;
        this.soumissionDossierRepository = soumissionDossierRepository;
        this.justiciableRepository = justiciableRepository;
        this.avocatRepository = avocatRepository;
        this.authenticationContext = authenticationContext;
    }

    public DossierResponseDTO createDossier(CreateDossierRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Justiciable justiciable = justiciableRepository.findById(uid)
                .orElseThrow(() -> new ForbiddenException("Seul un justiciable peut créer un dossier juridique"));

        Dossier dossier = new Dossier();
        dossier.setTitre(request.getTitre());
        dossier.setDescription(request.getDescription());
        dossier.setDateOuverture(LocalDate.now());
        dossier.setStatut("OUVERT");
        dossier.setJusticiable(justiciable);

        Dossier saved = dossierRepository.save(dossier);
        return toDossierDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DossierResponseDTO> getMyDossiers() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return dossierRepository.findByJusticiableIdOrderByDateOuvertureDesc(uid).stream()
                .map(this::toDossierDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DossierResponseDTO getDossierById(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        boolean isOwner = dossier.getJusticiable() != null && dossier.getJusticiable().getId().equals(uid);
        boolean isSubmittedAvocat = dossier.getSoumissions().stream()
                .anyMatch(s -> s.getAvocat() != null && s.getAvocat().getId().equals(uid));

        if (!isOwner && !isSubmittedAvocat) {
            throw new ForbiddenException("Accès non autorisé à ce dossier juridique");
        }

        return toDossierDto(dossier);
    }

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

        boolean dejaSoumis = dossier.getSoumissions().stream()
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

    @Transactional(readOnly = true)
    public List<SoumissionDossierResponseDTO> getSoumissionsPourAvocat(String statut) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        List<SoumissionDossier> list;
        if (statut != null && !statut.isBlank()) {
            list = soumissionDossierRepository.findByAvocatIdAndStatutOrderByDateSoumissionDesc(uid, statut);
        } else {
            list = soumissionDossierRepository.findByAvocatIdOrderByDateSoumissionDesc(uid);
        }
        return list.stream().map(this::toSoumissionDto).toList();
    }

    public SoumissionDossierResponseDTO repondreSoumission(String soumissionId, RepondreSoumissionRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        SoumissionDossier soumission = soumissionDossierRepository.findById(soumissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Soumission introuvable"));

        if (!soumission.getAvocat().getId().equals(uid)) {
            throw new ForbiddenException("Vous n'êtes pas l'avocat destinataire de cette soumission");
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

    private DossierResponseDTO toDossierDto(Dossier d) {
        List<PieceJointeDTO> pjDtos = d.getPiecesJointes() != null
                ? d.getPiecesJointes().stream()
                    .map(pj -> new PieceJointeDTO(pj.getId(), pj.getNom(), pj.getUrl(), pj.getDateAjout()))
                    .toList()
                : Collections.emptyList();

        List<EcheanceDTO> echDtos = d.getEcheances() != null
                ? d.getEcheances().stream()
                    .map(ech -> new EcheanceDTO(ech.getId(), ech.getDescription(), ech.getDate(), ech.getStatut()))
                    .toList()
                : Collections.emptyList();

        List<SoumissionDossierResponseDTO> soumDtos = d.getSoumissions() != null
                ? d.getSoumissions().stream().map(this::toSoumissionDto).toList()
                : Collections.emptyList();

        return DossierResponseDTO.builder()
                .id(d.getId())
                .titre(d.getTitre())
                .description(d.getDescription())
                .dateOuverture(d.getDateOuverture())
                .statut(d.getStatut())
                .justiciableId(d.getJusticiable() != null ? d.getJusticiable().getId() : null)
                .piecesJointes(pjDtos)
                .echeances(echDtos)
                .soumissions(soumDtos)
                .build();
    }

    private SoumissionDossierResponseDTO toSoumissionDto(SoumissionDossier s) {
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
