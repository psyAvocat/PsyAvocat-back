package com.psyavocat.repository;

import com.psyavocat.entity.FichePatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FichePatientRepository extends JpaRepository<FichePatient, String> {

    List<FichePatient> findByPsychologueIdOrderByDateCreationDesc(String psychologueId);

    Optional<FichePatient> findByPsychologueIdAndPatientId(String psychologueId, String patientId);
}
