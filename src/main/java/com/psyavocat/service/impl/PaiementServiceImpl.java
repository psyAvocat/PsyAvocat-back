package com.psyavocat.service.impl;

import com.psyavocat.entity.Paiement;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.entity.Utilisateur;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.repository.PaiementRepository;
import com.psyavocat.service.PaiementService;
import com.psyavocat.service.support.Entites;
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
    private final com.psyavocat.security.AuthenticationContext authenticationContext;

    public PaiementServiceImpl(PaiementRepository paiementRepository,
                               com.psyavocat.security.AuthenticationContext authenticationContext) {
        this.paiementRepository = paiementRepository;
        this.authenticationContext = authenticationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.psyavocat.dto.paiement.PaiementResponseDTO> getMesPaiements() {
        String uid = authenticationContext.getRequiredFirebaseUid();
        return paiementRepository.findByUtilisateurIdOrderByDatePaiementDesc(uid).stream()
                .map(this::toDto)
                .toList();
    }

    private com.psyavocat.dto.paiement.PaiementResponseDTO toDto(Paiement p) {
        var builder = com.psyavocat.dto.paiement.PaiementResponseDTO.builder()
                .id(p.getId())
                .montant(p.getMontant())
                .devise("XOF")
                .datePaiement(p.getDatePaiement())
                .statut(p.getStatut())
                .methode(p.getMethode())
                .simulation(p.getMethode() != null && p.getMethode().startsWith("SIMULATION"));
        RendezVous rdv = p.getRendezVous();
        if (rdv != null) {
            builder.rendezVousId(rdv.getId())
                    .rendezVousDate(rdv.getDateHeure())
                    .statutRendezVous(rdv.getStatut())
                    .montantTotalRendezVous(rdv.getMontantTotal())
                    .devise(rdv.getDevise() != null ? rdv.getDevise() : "XOF");
            if (rdv.getProfessionnel() != null) {
                com.psyavocat.entity.Professionnel proReel = Entites.reel(rdv.getProfessionnel());
                boolean estAvocat = (proReel instanceof com.psyavocat.entity.Avocat) || (rdv.getSoumission() != null);
                builder.professionnelNom(proReel.getNom())
                        .professionnelPrenom(proReel.getPrenom())
                        .typeProfessionnel(estAvocat ? "AVOCAT" : "PSYCHOLOGUE");
            }
        }
        return builder.build();
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
