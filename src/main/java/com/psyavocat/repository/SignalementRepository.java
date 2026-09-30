package com.psyavocat.repository;

import com.psyavocat.entity.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement, String> {
    List<Signalement> findByStatut(String statut);
}
