package com.psyavocat.repository;

import com.psyavocat.entity.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RendezVousRepository extends JpaRepository<RendezVous, String> {

    List<RendezVous> findByPatientIdOrderByDateHeureDesc(String patientId);

    List<RendezVous> findByProfessionnelIdOrderByDateHeureDesc(String professionnelId);
}
