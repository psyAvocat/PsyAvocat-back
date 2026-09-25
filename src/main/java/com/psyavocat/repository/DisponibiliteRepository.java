package com.psyavocat.repository;

import com.psyavocat.entity.Disponibilite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisponibiliteRepository extends JpaRepository<Disponibilite, String> {

    List<Disponibilite> findByProfessionnelId(String professionnelId);

    List<Disponibilite> findByProfessionnelIdAndStatut(String professionnelId, String statut);

    List<Disponibilite> findByProfessionnelIdAndDateGreaterThanEqualOrderByDateAscHeureDebutAsc(String professionnelId, LocalDate date);
}
