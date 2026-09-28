package com.psyavocat.service;

import com.psyavocat.entity.Paiement;
import com.psyavocat.entity.RendezVous;
import com.psyavocat.entity.Utilisateur;

import java.math.BigDecimal;

/**
 * Contrat de service pour la gestion et la simulation des paiements.
 * Conformément aux règles métier PsyAvocat, une seule entité Paiement est utilisée
 * pour représenter les paiements de rendez-vous et d'abonnements.
 */
public interface PaiementService {

    /**
     * Calcule le montant de l'acompte obligatoire de 20 % pour un rendez-vous.
     *
     * @param montantTotal Montant total de la consultation
     * @return Montant calculé correspondant à 20 % du total
     */
    BigDecimal calculerAcompteRendezVous(BigDecimal montantTotal);

    /**
     * Traite et enregistre le paiement de l'acompte simulé pour un rendez-vous.
     *
     * @param utilisateur Utilisateur payeur (patient ou justiciable)
     * @param rdv Rendez-vous rattaché
     * @param montantAcompte Montant de l'acompte payé
     * @return L'entité Paiement enregistrée
     */
    Paiement traiterAcompteRendezVous(Utilisateur utilisateur, RendezVous rdv, BigDecimal montantAcompte);

    /**
     * Simule une transaction de paiement générique (ex: abonnement ou paiement direct).
     *
     * @param utilisateur Utilisateur effectuant le paiement
     * @param montant Montant de la transaction
     * @param methode Méthode de paiement (ex: SIMULATION_CARTE)
     * @return L'entité Paiement enregistrée
     */
    Paiement simulerPaiement(Utilisateur utilisateur, BigDecimal montant, String methode);
}
