package com.psyavocat.repository;

import com.psyavocat.entity.Psychologue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PsychologueRepository extends JpaRepository<Psychologue, String> {

    List<Psychologue> findByStatutValidation(String statutValidation);
}
