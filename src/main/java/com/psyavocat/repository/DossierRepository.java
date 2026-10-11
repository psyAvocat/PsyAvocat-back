package com.psyavocat.repository;

import com.psyavocat.entity.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, String> {

    List<Dossier> findByJusticiableIdOrderByDateOuvertureDesc(String justiciableId);

    boolean existsBySpecialiteId(String specialiteId);

    /**
     * Dossier relu sous verrou exclusif : sérialise les soumissions et les acceptations
     * concurrentes (deux avocats acceptant au même instant, double soumission).
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT d FROM Dossier d WHERE d.id = :id")
    java.util.Optional<Dossier> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") String id);
}
