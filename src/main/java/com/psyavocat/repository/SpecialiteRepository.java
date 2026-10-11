package com.psyavocat.repository;

import com.psyavocat.entity.Specialite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialiteRepository extends JpaRepository<Specialite, String> {
    Optional<Specialite> findByNomIgnoreCase(String nom);
    boolean existsByNomIgnoreCase(String nom);
    long countByDomaineId(String domaineId);

    /** Nombre de professionnels rattachés à la spécialité. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(p) FROM Professionnel p JOIN p.specialites s WHERE s.id = :id")
    long countProfessionnels(@org.springframework.data.repository.query.Param("id") String specialiteId);

    java.util.List<Specialite> findByTypeProfessionnel(String typeProfessionnel);

}
