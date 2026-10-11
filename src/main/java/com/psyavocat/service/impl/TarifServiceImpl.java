package com.psyavocat.service.impl;

import com.psyavocat.dto.referentiel.TarifProfessionnelDTO;
import com.psyavocat.dto.referentiel.TarifRequest;
import com.psyavocat.entity.Professionnel;
import com.psyavocat.entity.TarifProfessionnel;
import com.psyavocat.exception.ForbiddenException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.TarifProfessionnelRepository;
import com.psyavocat.service.TarifService;
import com.psyavocat.service.support.AccesProfessionnel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Gestion des tarifs par le professionnel. Lecture : tout professionnel connecté (même suspendu,
 * pour consulter son profil). Écriture : comptes en attente de vérification ou approuvés.
 * Aucun prix n'est fixé par défaut : seuls les montants saisis par le professionnel existent.
 */
@Service
@Transactional
public class TarifServiceImpl implements TarifService {

    static final String DEVISE = "XOF";
    private static final int DUREE_PAR_DEFAUT = 45;

    private final TarifProfessionnelRepository tarifRepository;
    private final AccesProfessionnel accesProfessionnel;
    private final RealtimeGateway realtimeGateway;

    public TarifServiceImpl(TarifProfessionnelRepository tarifRepository,
                            AccesProfessionnel accesProfessionnel,
                            RealtimeGateway realtimeGateway) {
        this.tarifRepository = tarifRepository;
        this.accesProfessionnel = accesProfessionnel;
        this.realtimeGateway = realtimeGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifProfessionnelDTO> getMesTarifs() {
        Professionnel pro = accesProfessionnel.professionnelConnecte();
        return tarifRepository.findByProfessionnelId(pro.getId()).stream()
                .sorted(Comparator.comparing(TarifProfessionnel::getMontant))
                .map(TarifServiceImpl::toDto)
                .toList();
    }

    @Override
    public TarifProfessionnelDTO creer(TarifRequest request) {
        Professionnel pro = accesProfessionnel.exigerParcoursVerificationOuvert();
        TarifProfessionnel tarif = new TarifProfessionnel();
        tarif.setProfessionnel(pro);
        tarif.setDevise(DEVISE);
        appliquer(tarif, request);
        TarifProfessionnel saved = tarifRepository.save(tarif);
        signaler(pro);
        return toDto(saved);
    }

    @Override
    public TarifProfessionnelDTO modifier(String id, TarifRequest request) {
        Professionnel pro = accesProfessionnel.exigerParcoursVerificationOuvert();
        TarifProfessionnel tarif = tarifDuProfessionnel(id, pro);
        appliquer(tarif, request);
        TarifProfessionnel saved = tarifRepository.save(tarif);
        signaler(pro);
        return toDto(saved);
    }

    @Override
    public TarifProfessionnelDTO changerPublication(String id, boolean actif) {
        Professionnel pro = accesProfessionnel.exigerParcoursVerificationOuvert();
        TarifProfessionnel tarif = tarifDuProfessionnel(id, pro);
        tarif.setActif(actif);
        TarifProfessionnel saved = tarifRepository.save(tarif);
        signaler(pro);
        return toDto(saved);
    }

    /**
     * Les rendez-vous enregistrent leur propre montant : supprimer un tarif ne modifie
     * aucun rendez-vous déjà réservé.
     */
    @Override
    public void supprimer(String id) {
        Professionnel pro = accesProfessionnel.exigerParcoursVerificationOuvert();
        TarifProfessionnel tarif = tarifDuProfessionnel(id, pro);
        pro.getTarifs().remove(tarif);
        tarifRepository.delete(tarif);
        signaler(pro);
    }

    private TarifProfessionnel tarifDuProfessionnel(String id, Professionnel pro) {
        TarifProfessionnel tarif = tarifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif introuvable"));
        if (tarif.getProfessionnel() == null || !pro.getId().equals(tarif.getProfessionnel().getId())) {
            throw new ForbiddenException("Ce tarif ne vous appartient pas");
        }
        return tarif;
    }

    private void appliquer(TarifProfessionnel tarif, TarifRequest request) {
        tarif.setTitre(request.getTitre().trim());
        tarif.setMontant(request.getMontant());
        tarif.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription().trim() : null);
        tarif.setDureeMinutes(request.getDureeMinutes() != null ? request.getDureeMinutes() : DUREE_PAR_DEFAUT);
        if (request.getActif() != null) {
            tarif.setActif(request.getActif());
        } else if (tarif.getActif() == null) {
            tarif.setActif(true);
        }
    }

    /** La fiche publique du professionnel (tarifs) est rechargée par les clients connectés. */
    private void signaler(Professionnel pro) {
        realtimeGateway.diffuser("PROFESSIONNEL_MIS_A_JOUR", Map.of("id", pro.getId()));
    }

    static TarifProfessionnelDTO toDto(TarifProfessionnel t) {
        return TarifProfessionnelDTO.builder()
                .id(t.getId())
                .titre(t.getTitre())
                .montant(t.getMontant())
                .devise(t.getDevise())
                .description(t.getDescription())
                .dureeMinutes(t.getDureeMinutes())
                .actif(!Boolean.FALSE.equals(t.getActif()))
                .build();
    }
}
