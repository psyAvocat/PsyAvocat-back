package com.psyavocat.repository;

import com.psyavocat.entity.ResultatOrientation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultatOrientationRepository extends JpaRepository<ResultatOrientation, String> {

    List<ResultatOrientation> findByPatientIdOrderByDateEvaluationDesc(String patientId);
}
