package com.psyavocat.repository;

import com.psyavocat.entity.Contenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContenuRepository extends JpaRepository<Contenu, String> {

    /**
     * Contenus publiés d'un type : actifs, auteur actif et validé.
     * Filtres facultatifs : texte (titre / description) et spécialité.
     */
    @Query("SELECT c FROM Contenu c JOIN c.auteur a WHERE c.type = :type AND c.actif = true "
            + "AND a.actif = true AND TYPE(a) IN (Avocat, Psychologue) "
            + "AND (:specialiteId IS NULL OR c.specialite.id = :specialiteId) "
            + "AND (:q IS NULL OR LOWER(c.titre) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "     OR LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Contenu> rechercherPublies(@Param("type") String type,
                                    @Param("q") String q,
                                    @Param("specialiteId") String specialiteId);

    List<Contenu> findByAuteurIdOrderByDatePublicationDesc(String auteurId);

    @Modifying
    @Query("UPDATE Contenu c SET c.nombreVues = COALESCE(c.nombreVues, 0) + 1 WHERE c.id = :id")
    void incrementerVues(@Param("id") String id);
}
