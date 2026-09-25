package com.psyavocat.repository;

import com.psyavocat.entity.Seance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeanceRepository extends JpaRepository<Seance, String> {

    List<Seance> findByPsychologueIdOrderByDateDesc(String psychologueId);

    List<Seance> findByFichePatientIdOrderByDateDesc(String fichePatientId);
}
