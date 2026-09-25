package com.psyavocat.repository;

import com.psyavocat.entity.Professionnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionnelRepository extends JpaRepository<Professionnel, String> {

    List<Professionnel> findByStatutValidation(String statutValidation);

    @Query("SELECT DISTINCT p FROM Professionnel p LEFT JOIN p.specialites s WHERE " +
           "(:statut IS NULL OR p.statutValidation = :statut) AND " +
           "(:ville IS NULL OR LOWER(p.ville) LIKE LOWER(CONCAT('%', :ville, '%'))) AND " +
           "(:modeConsultation IS NULL OR p.modeConsultation = :modeConsultation) AND " +
           "(:specialiteId IS NULL OR s.id = :specialiteId)")
    List<Professionnel> searchProfessionnels(
            @Param("statut") String statut,
            @Param("ville") String ville,
            @Param("modeConsultation") String modeConsultation,
            @Param("specialiteId") String specialiteId
    );
}
