package com.psyavocat.repository;

import com.psyavocat.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, String> {

    List<Paiement> findByUtilisateurIdOrderByDatePaiementDesc(String utilisateurId);
}
