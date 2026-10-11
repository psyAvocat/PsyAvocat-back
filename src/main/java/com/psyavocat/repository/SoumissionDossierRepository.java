package com.psyavocat.repository;

import com.psyavocat.entity.SoumissionDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoumissionDossierRepository extends JpaRepository<SoumissionDossier, String> {

    List<SoumissionDossier> findByDossierId(String dossierId);

    /** L'avocat a-t-il reçu au moins un dossier de ce justiciable ? */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) > 0 FROM SoumissionDossier s "
            + "WHERE s.avocat.id = :avocatId AND s.dossier.justiciable.id = :justiciableId")
    boolean existsByAvocatIdAndDossierJusticiableId(
            @org.springframework.data.repository.query.Param("avocatId") String avocatId,
            @org.springframework.data.repository.query.Param("justiciableId") String justiciableId);

    List<SoumissionDossier> findByAvocatIdOrderByDateSoumissionDesc(String avocatId);

    List<SoumissionDossier> findByAvocatIdAndStatutOrderByDateSoumissionDesc(String avocatId, String statut);

    @org.springframework.data.jpa.repository.Query("SELECT s.statut, COUNT(s) FROM SoumissionDossier s WHERE s.avocat.id = :avocatId GROUP BY s.statut")
    List<Object[]> compterParStatutPourAvocat(@org.springframework.data.repository.query.Param("avocatId") String avocatId);
}
