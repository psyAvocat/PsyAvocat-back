package com.psyavocat.repository;

import com.psyavocat.entity.CategorieBesoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieBesoinRepository extends JpaRepository<CategorieBesoin, String> {

    List<CategorieBesoin> findByActifTrue();

    List<CategorieBesoin> findByActifTrueAndTypeProfessionnel(String typeProfessionnel);

    Optional<CategorieBesoin> findByNom(String nom);

    boolean existsByNom(String nom);

    Optional<CategorieBesoin> findByCode(String code);

    boolean existsByCode(String code);
}
