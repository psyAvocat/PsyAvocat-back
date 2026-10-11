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
import com.psyavocat.service.NotificationService;
import com.psyavocat.service.SoumissionDossierService;
import com.psyavocat.service.notification.NotificationEvenement;
import com.psyavocat.service.support.AccesProfessionnel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Workflow juridique : dossier → soumission à un ou plusieurs avocats → prise en charge → rendez-vous.
 *
 * - Un dossier ouvert peut être soumis à plusieurs avocats approuvés (une seule fois par avocat).
 * - Chaque avocat répond à SA soumission : ACCEPTEE (honoraires obligatoires) ou REFUSEE (motif obligatoire) ;
 *   il peut aussi ne pas répondre (la soumission reste EN_ATTENTE).
 * - La première acceptation prend en charge le dossier de façon exclusive : les autres soumissions
 *   deviennent CADUQUE et ne sont plus accessibles aux confrères.
 * - Le dossier est relu sous verrou exclusif : deux acceptations ou soumissions simultanées sont sérialisées.
 */
@Service
@Transactional
public class SoumissionDossierServiceImpl implements SoumissionDossierService {

    static final String EN_ATTENTE = "EN_ATTENTE";
    static final String ACCEPTEE = "ACCEPTEE";
    static final String REFUSEE = "REFUSEE";
    static final String CADUQUE = "CADUQUE";
    static final String DOSSIER_PRIS_EN_CHARGE = "PRIS_EN_CHARGE";

    private static final String UNIVERS_AVOCAT = "AVOCAT";
    static final String RESSOURCE_DOSSIER = "DOSSIER";
    static final String RESSOURCE_SOUMISSION = "SOUMISSION";

    private final DossierRepository dossierRepository;
    private final SoumissionDossierRepository soumissionDossierRepository;
    private final AvocatRepository avocatRepository;
    private final AuthenticationContext authenticationContext;
    private final AccesProfessionnel accesProfessionnel;
    private final NotificationService notificationService;

    public SoumissionDossierServiceImpl(
            DossierRepository dossierRepository,
            SoumissionDossierRepository soumissionDossierRepository,
            AvocatRepository avocatRepository,
            AuthenticationContext authenticationContext,
            AccesProfessionnel accesProfessionnel,
            NotificationService notificationService
    ) {
        this.dossierRepository = dossierRepository;
        this.soumissionDossierRepository = soumissionDossierRepository;
        this.avocatRepository = avocatRepository;
        this.authenticationContext = authenticationContext;
        this.accesProfessionnel = accesProfessionnel;
        this.notificationService = notificationService;
    }

    @Override
    public SoumissionDossierResponseDTO soumettreDossier(String dossierId, SoumissionDossierRequest request) {
        String uid = authenticationContext.getRequiredFirebaseUid();
        Dossier dossier = dossierRepository.findByIdForUpdate(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        if (dossier.getJusticiable() == null || !dossier.getJusticiable().getId().equals(uid)) {
            throw new ForbiddenException("Vous devez être le propriétaire du dossier pour le soumettre");
        }
        if (DOSSIER_PRIS_EN_CHARGE.equalsIgnoreCase(dossier.getStatut())) {
            throw new BadRequestException("Ce dossier est déjà pris en charge par un avocat : il ne peut plus être soumis");
        }

        Avocat avocat = avocatRepository.findById(request.getAvocatId())
                .orElseThrow(() -> new ResourceNotFoundException("Avocat introuvable"));

        if (!AccesProfessionnel.estApprouve(avocat)) {
            throw new BadRequestException("Cet avocat n'est pas disponible sur la plateforme");
        }

        boolean dejaSoumis = soumissionDossierRepository.findByDossierId(dossier.getId()).stream()
                .anyMatch(s -> s.getAvocat() != null && s.getAvocat().getId().equals(avocat.getId()));
        if (dejaSoumis) {
            throw new ConflictException("Ce dossier a déjà été soumis à cet avocat");
        }

        SoumissionDossier soumission = new SoumissionDossier();
        soumission.setDossier(dossier);
        soumission.setAvocat(avocat);
        soumission.setDateSoumission(LocalDateTime.now());
        soumission.setStatut(EN_ATTENTE);

        SoumissionDossier saved = soumissionDossierRepository.save(soumission);

        notifier(avocat.getId(), "DOSSIER_RECU", "Nouveau dossier reçu",
                nomClient(dossier) + " vous a soumis le dossier « " + dossier.getTitre() + " ».",
                RESSOURCE_SOUMISSION, saved.getId());
        return toSoumissionDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoumissionDossierResponseDTO> getSoumissionsPourAvocat(String statut) {
        String uid = accesProfessionnel.exigerAvocatApprouve().getId();
        List<SoumissionDossier> list;
        if (statut != null && !statut.isBlank()) {
            list = soumissionDossierRepository.findByAvocatIdAndStatutOrderByDateSoumissionDesc(uid, statut);
        } else {
            // Par défaut, ne liste que les soumissions actives (exclut les dossiers devenus CADUQUE car pris par un autre confrère)
            list = soumissionDossierRepository.findByAvocatIdOrderByDateSoumissionDesc(uid).stream()
                    .filter(s -> !CADUQUE.equalsIgnoreCase(s.getStatut()))
                    .toList();
        }
        return list.stream().map(this::toSoumissionDto).toList();
    }

    @Override
    public SoumissionDossierResponseDTO repondreSoumission(String soumissionId, RepondreSoumissionRequest request) {
        String uid = accesProfessionnel.exigerAvocatApprouve().getId();
        SoumissionDossier soumission = soumissionDossierRepository.findById(soumissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Soumission introuvable"));

        if (soumission.getAvocat() == null || !soumission.getAvocat().getId().equals(uid)) {
            throw new ForbiddenException("Vous n'êtes pas l'avocat destinataire de cette soumission");
        }

        // Verrou sur le dossier : une seule prise en charge, même en cas d'acceptations simultanées.
        Dossier dossier = soumission.getDossier() != null
                ? dossierRepository.findByIdForUpdate(soumission.getDossier().getId()).orElse(soumission.getDossier())
                : null;

        if (CADUQUE.equalsIgnoreCase(soumission.getStatut()) ||
            (dossier != null && DOSSIER_PRIS_EN_CHARGE.equalsIgnoreCase(dossier.getStatut()))) {
            throw new BadRequestException("Ce dossier juridique a déjà été confié et pris en charge par un autre confrère");
        }
        if (soumission.getStatut() != null && !EN_ATTENTE.equalsIgnoreCase(soumission.getStatut())) {
            throw new BadRequestException("Vous avez déjà répondu à cette soumission");
        }

        String statut = request.getStatut().trim().toUpperCase();
        if (!ACCEPTEE.equals(statut) && !REFUSEE.equals(statut)) {
            throw new BadRequestException("Statut invalide. Valeurs possibles : ACCEPTEE, REFUSEE");
        }

        String reponse = request.getReponse() != null && !request.getReponse().isBlank() ? request.getReponse().trim() : null;
        if (ACCEPTEE.equals(statut)) {
            if (request.getTarifPropose() == null || request.getTarifPropose().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Un tarif proposé supérieur à 0 est requis en cas d'acceptation du dossier");
            }
            soumission.setTarifPropose(request.getTarifPropose());
        } else if (reponse == null) {
            throw new BadRequestException("Indiquez le motif du refus pour informer le justiciable");
        }

        soumission.setStatut(statut);
        soumission.setReponse(reponse);
        soumission.setDateReponse(LocalDateTime.now());
        SoumissionDossier saved = soumissionDossierRepository.save(soumission);

        if (dossier != null && ACCEPTEE.equals(statut)) {
            prendreEnCharge(dossier, saved);
        }

        if (dossier != null && dossier.getJusticiable() != null) {
            String avocatNom = "Me " + saved.getAvocat().getPrenom() + " " + saved.getAvocat().getNom();
            if (ACCEPTEE.equals(statut)) {
                notifier(dossier.getJusticiable().getId(), "DOSSIER_ACCEPTE", "Dossier accepté",
                        avocatNom + " accepte de prendre en charge votre dossier « " + dossier.getTitre()
                                + " ». Vous pouvez maintenant prendre rendez-vous.",
                        RESSOURCE_DOSSIER, dossier.getId());
            } else {
                notifier(dossier.getJusticiable().getId(), "DOSSIER_REFUSE", "Réponse à votre dossier",
                        avocatNom + " ne peut pas prendre en charge votre dossier « " + dossier.getTitre() + " ».",
                        RESSOURCE_DOSSIER, dossier.getId());
            }
        }
        return toSoumissionDto(saved);
    }

    /** Prise en charge exclusive : le dossier est confié, les autres soumissions deviennent caduques. */
    private void prendreEnCharge(Dossier dossier, SoumissionDossier acceptee) {
        dossier.setStatut(DOSSIER_PRIS_EN_CHARGE);
        dossierRepository.save(dossier);
        for (SoumissionDossier autre : soumissionDossierRepository.findByDossierId(dossier.getId())) {
            if (autre.getId().equals(acceptee.getId()) || CADUQUE.equalsIgnoreCase(autre.getStatut())) {
                continue;
            }
            boolean enAttente = EN_ATTENTE.equalsIgnoreCase(autre.getStatut());
            autre.setStatut(CADUQUE);
            soumissionDossierRepository.save(autre);
            if (enAttente && autre.getAvocat() != null) {
                notifier(autre.getAvocat().getId(), "DOSSIER_CADUQUE", "Dossier confié à un confrère",
                        "Le dossier « " + dossier.getTitre() + " » a été pris en charge par un autre avocat.",
                        null, null);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoumissionDossierResponseDTO> getSoumissionsParDossier(String dossierId) {
        return soumissionDossierRepository.findByDossierId(dossierId)
                .stream()
                .map(this::toSoumissionDto)
                .toList();
    }

    private void notifier(String destinataireId, String type, String titre, String message,
                          String ressourceType, String ressourceId) {
        notificationService.notifier(new NotificationEvenement(
                destinataireId, type, titre, message, UNIVERS_AVOCAT, ressourceType, ressourceId, null));
    }

    private String nomClient(Dossier dossier) {
        return dossier.getJusticiable() != null
                ? dossier.getJusticiable().getPrenom() + " " + dossier.getJusticiable().getNom()
                : "Un justiciable";
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
