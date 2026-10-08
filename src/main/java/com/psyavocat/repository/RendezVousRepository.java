package com.psyavocat.repository;

import com.psyavocat.entity.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RendezVousRepository extends JpaRepository<RendezVous, String> {

    List<RendezVous> findByPatientIdOrderByDateHeureDesc(String patientId);

    List<RendezVous> findByProfessionnelIdOrderByDateHeureDesc(String professionnelId);

    /** Rendez-vous d'un statut donné dont le début est dans [debut, fin[ (rappels planifiés). */
    List<RendezVous> findByStatutAndDateHeureGreaterThanEqualAndDateHeureLessThan(
            String statut, LocalDateTime debut, LocalDateTime fin);

    /** Nombre de rendez-vous dont la date est dans [debut, fin[. */
    long countByDateHeureGreaterThanEqualAndDateHeureLessThan(LocalDateTime debut, LocalDateTime fin);

    /**
     * Nombre de rendez-vous par mois civil sur [debut, fin[.
     * Chaque ligne : [annee (Integer), mois 1-12 (Integer), total (Long)].
     */
    @Query("SELECT YEAR(r.dateHeure), MONTH(r.dateHeure), COUNT(r) FROM RendezVous r " +
           "WHERE r.dateHeure >= :debut AND r.dateHeure < :fin " +
           "GROUP BY YEAR(r.dateHeure), MONTH(r.dateHeure)")
    List<Object[]> compterRendezVousParMois(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** Chaque ligne : [statut (String, peut être null), total (Long)]. */
    @Query("SELECT r.statut, COUNT(r) FROM RendezVous r GROUP BY r.statut")
    List<Object[]> compterParStatut();

    /**
     * Nombre de rendez-vous par mois civil pour un professionnel spécifique sur [debut, fin[.
     */
    @Query("SELECT YEAR(r.dateHeure), MONTH(r.dateHeure), COUNT(r) FROM RendezVous r " +
           "WHERE r.professionnel.id = :proId AND r.dateHeure >= :debut AND r.dateHeure < :fin " +
           "GROUP BY YEAR(r.dateHeure), MONTH(r.dateHeure)")
    List<Object[]> compterRendezVousParMoisPourProfessionnel(@Param("proId") String proId, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /** Statuts des rendez-vous pour un professionnel. */
    @Query("SELECT r.statut, COUNT(r) FROM RendezVous r WHERE r.professionnel.id = :proId GROUP BY r.statut")
    List<Object[]> compterParStatutPourProfessionnel(@Param("proId") String proId);

    /** Modes de consultation (CABINET, VISIO) pour un professionnel. */
    @Query("SELECT r.mode, COUNT(r) FROM RendezVous r WHERE r.professionnel.id = :proId GROUP BY r.mode")
    List<Object[]> compterParModePourProfessionnel(@Param("proId") String proId);
}
