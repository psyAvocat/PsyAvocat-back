package com.psyavocat.service.support;

import com.psyavocat.dto.admin.DecisionAdministrativeDTO;
import com.psyavocat.entity.DecisionAdministrative;
import com.psyavocat.repository.DecisionAdministrativeRepository;
import com.psyavocat.security.AuthenticationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** Enregistre et restitue l'historique des décisions administratives. */
@Component
public class JournalDecisions {

    public static final String TYPE_STATUT_PROFESSIONNEL = "STATUT_PROFESSIONNEL";
    public static final String TYPE_SIGNALEMENT = "SIGNALEMENT";
    public static final String TYPE_JUSTIFICATIF = "JUSTIFICATIF";

    private final DecisionAdministrativeRepository repository;
    private final AuthenticationContext authenticationContext;

    public JournalDecisions(DecisionAdministrativeRepository repository, AuthenticationContext authenticationContext) {
        this.repository = repository;
        this.authenticationContext = authenticationContext;
    }

    public void enregistrer(String type, String cibleId, String decision, String motif) {
        DecisionAdministrative d = new DecisionAdministrative();
        d.setType(type);
        d.setCibleId(cibleId);
        d.setDecision(decision);
        d.setMotif(motif != null && !motif.isBlank() ? motif.trim() : null);
        d.setAdministrateurId(authenticationContext.getFirebaseUid().orElse(null));
        d.setDateDecision(LocalDateTime.now());
        repository.save(d);
    }

    /** Dernière décision enregistrée pour un élément (null s'il n'a jamais été examiné). */
    public DecisionAdministrativeDTO derniere(String cibleId) {
        List<DecisionAdministrativeDTO> decisions = historique(cibleId);
        return decisions.isEmpty() ? null : decisions.get(0);
    }

    public List<DecisionAdministrativeDTO> historique(String cibleId) {
        return repository.findByCibleIdOrderByDateDecisionDesc(cibleId).stream()
                .map(d -> DecisionAdministrativeDTO.builder()
                        .id(d.getId())
                        .type(d.getType())
                        .cibleId(d.getCibleId())
                        .decision(d.getDecision())
                        .motif(d.getMotif())
                        .administrateurId(d.getAdministrateurId())
                        .dateDecision(d.getDateDecision())
                        .build())
                .toList();
    }
}
