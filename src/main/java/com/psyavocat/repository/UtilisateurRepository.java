package com.psyavocat.repository;

import com.psyavocat.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Utilisateur.
 * L'identifiant primaire `id` correspond directement au Firebase UID.
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, String> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Identifiants et téléphones renseignés : [id (String), telephone (String)]. */
    @Query("SELECT u.id, u.telephone FROM Utilisateur u WHERE u.telephone IS NOT NULL")
    List<Object[]> findAllTelephones();

    /**
     * Nombre d'inscriptions par mois civil sur l'intervalle [debut, fin].
     * Chaque ligne : [annee (Integer), mois 1-12 (Integer), total (Long)].
     */
    @Query("SELECT YEAR(u.dateInscription), MONTH(u.dateInscription), COUNT(u) FROM Utilisateur u " +
           "WHERE u.dateInscription >= :debut AND u.dateInscription <= :fin " +
           "GROUP BY YEAR(u.dateInscription), MONTH(u.dateInscription)")
    List<Object[]> compterInscriptionsParMois(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
