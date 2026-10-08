package com.psyavocat.repository;

import com.psyavocat.entity.ParametrePlateforme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametrePlateformeRepository extends JpaRepository<ParametrePlateforme, String> {
}
