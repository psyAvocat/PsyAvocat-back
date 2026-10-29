package com.psyavocat.repository;

import com.psyavocat.entity.LectureContenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LectureContenuRepository extends JpaRepository<LectureContenu, String> {

    /**
     * Enregistre la première lecture de ce lecteur ; ignorée s'il a déjà lu la publication
     * (contrainte d'unicité, sans erreur même pour deux ouvertures simultanées).
     *
     * @return 1 pour une première lecture, 0 sinon
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO contenu_lectures (id, contenu_id, lecteur_id, date_lecture) "
            + "VALUES (:id, :contenuId, :lecteurId, :date)", nativeQuery = true)
    int enregistrerPremiereLecture(@Param("id") String id,
                                   @Param("contenuId") String contenuId,
                                   @Param("lecteurId") String lecteurId,
                                   @Param("date") LocalDateTime date);

    @Modifying
    @Query("DELETE FROM LectureContenu l WHERE l.contenu.id = :contenuId")
    void supprimerPourContenu(@Param("contenuId") String contenuId);
}
