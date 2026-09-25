package com.psyavocat.repository;

import com.psyavocat.entity.Echeance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EcheanceRepository extends JpaRepository<Echeance, String> {

    List<Echeance> findByDossierIdOrderByDateAsc(String dossierId);
}
