package com.psyavocat.service.impl;

import com.psyavocat.entity.Paiement;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.repository.PaiementRepository;
import com.psyavocat.service.PaiementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Transactional
public class PaiementServiceImpl implements PaiementService {

    public static final BigDecimal TAUX_ACOMPTE_RDV = new BigDecimal("0.20");

    private final PaiementRepository paiementRepository;

    public PaiementServiceImpl(PaiementRepository paiementRepository) {
        this.paiementRepository = paiementRepository;
    }

    @Override
    public BigDecimal calculerAcompteRendezVous(BigDecimal montantTotal) {
        if (montantTotal == null || montantTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Le montant total du rendez-vous est invalide");
        }
        return montantTotal.multiply(TAUX_ACOMPTE_RDV).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Paiement traiterAcompteRendezVous(Utilisateur utilisateur, RendezVous rdv, BigDecimal montantAcompte) {
        Paiement paiement = new Paiement();
        paiement.setMontant(montantAcompte);
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setStatut("PAYE");
        paiement.setMethode("SIMULATION_CARTE");
        paiement.setUtilisateur(utilisateur);
        paiement.setRendezVous(rdv);

        return paiementRepository.save(paiement);
    }

    @Override
    public Paiement simulerPaiement(Utilisateur utilisateur, BigDecimal montant, String methode) {
        Paiement paiement = new Paiement();
        paiement.setMontant(montant);
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setStatut("PAYE");
        paiement.setMethode(methode != null ? methode : "SIMULATION_CARTE");
        paiement.setUtilisateur(utilisateur);

        return paiementRepository.save(paiement);
    }
}
