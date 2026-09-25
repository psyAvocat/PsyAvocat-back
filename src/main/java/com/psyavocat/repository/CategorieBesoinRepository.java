package com.psyavocat.repository;

import com.psyavocat.entity.CategorieBesoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorieBesoinRepository extends JpaRepository<CategorieBesoin, String> {

    List<CategorieBesoin> findByActifTrue();

    List<CategorieBesoin> findByActifTrueAndTypeProfessionnel(String typeProfessionnel);
}
