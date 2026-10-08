package com.psyavocat.service.impl;

import com.psyavocat.dto.dossier.*;
import com.psyavocat.entity.Dossier;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.repository.DossierRepository;
import com.psyavocat.repository.UtilisateurRepository;
import com.psyavocat.security.AuthenticationContext;
import com.psyavocat.service.DossierService;
import com.psyavocat.service.SoumissionDossierService;
import com.psyavocat.service.support.ClientAccounts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class DossierServiceImpl implements DossierService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SoumissionDossierService soumissionDossierService;
    private final AuthenticationContext authenticationContext;

    public DossierServiceImpl(
            DossierRepository dossierRepository,
            UtilisateurRepository utilisateurRepository,
            SoumissionDossierService soumissionDossierService,
            AuthenticationContext authenticationContext
    ) {
        this.dossierRepository = dossierRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.soumissionDossierService = soumissionDossierService;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public DossierResponseDTO createDossier(CreateDossierRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Utilisateur justiciable = utilisateurRepository.findById(uid)
                .filter(ClientAccounts::isClient)
                .orElseThrow(() -> new ForbiddenException("Seul un client peut créer un dossier juridique"));

        Dossier dossier = new Dossier();
        dossier.setTitre(request.getTitre());
        dossier.setDescription(request.getDescription());
        dossier.setDateOuverture(LocalDate.now());
        dossier.setStatut("OUVERT");
        dossier.setJusticiable(justiciable);

        Dossier saved = dossierRepository.save(dossier);
        return toDossierDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DossierResponseDTO> getMyDossiers() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return dossierRepository.findByJusticiableIdOrderByDateOuvertureDesc(uid).stream()
                .map(this::toDossierDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DossierResponseDTO getDossierById(String id) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        boolean isOwner = dossier.getJusticiable() != null && dossier.getJusticiable().getId().equals(uid);
        boolean isSubmittedAvocatActif = dossier.getSoumissions() != null && dossier.getSoumissions().stream()
                .anyMatch(s -> s.getAvocat() != null 
                        && s.getAvocat().getId().equals(uid)
                        && !"CADUQUE".equalsIgnoreCase(s.getStatut())
                        && !"REFUSEE".equalsIgnoreCase(s.getStatut()));

        if (!isOwner && !isSubmittedAvocatActif) {
            throw new ForbiddenException("Accès non autorisé : ce dossier a été confié à un autre confrère ou n'est plus accessible");
        }

        return toDossierDto(dossier);
    }

    @Override
    public SoumissionDossierResponseDTO soumettreDossier(String dossierId, SoumissionDossierRequest request) {
        return soumissionDossierService.soumettreDossier(dossierId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoumissionDossierResponseDTO> getSoumissionsPourAvocat(String statut) {
        return soumissionDossierService.getSoumissionsPourAvocat(statut);
    }

    @Override
    public SoumissionDossierResponseDTO repondreSoumission(String soumissionId, RepondreSoumissionRequest request) {
        return soumissionDossierService.repondreSoumission(soumissionId, request);
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

        List<SoumissionDossierResponseDTO> soumDtos = d.getId() != null
                ? soumissionDossierService.getSoumissionsParDossier(d.getId())
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
}
