package com.psyavocat.repository;

import com.psyavocat.entity.ResultatOrientationCategorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultatOrientationCategorieRepository extends JpaRepository<ResultatOrientationCategorie, String> {

    List<ResultatOrientationCategorie> findByResultatOrientationIdOrderByRangAsc(String resultatOrientationId);
}
