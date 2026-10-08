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

    boolean existsByProfessionnelIdAndDateAndHeureDebut(String professionnelId, LocalDate date, java.time.LocalTime heureDebut);

    List<Disponibilite> findByProfessionnelIdAndDate(String professionnelId, LocalDate date);

    @org.springframework.data.jpa.repository.Query(
        "SELECT d FROM Disponibilite d WHERE d.professionnel.id = :proId AND d.date = :date AND d.heureDebut < :heureFin AND d.heureFin > :heureDebut"
    )
    List<Disponibilite> findConflictingDisponibilites(
            @org.springframework.data.repository.query.Param("proId") String professionnelId,
            @org.springframework.data.repository.query.Param("date") LocalDate date,
            @org.springframework.data.repository.query.Param("heureDebut") java.time.LocalTime heureDebut,
            @org.springframework.data.repository.query.Param("heureFin") java.time.LocalTime heureFin
    );

    long countByProfessionnelId(String professionnelId);
}
