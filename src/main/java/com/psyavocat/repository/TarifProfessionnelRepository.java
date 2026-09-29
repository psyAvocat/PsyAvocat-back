package com.psyavocat.repository;

import com.psyavocat.entity.TarifProfessionnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarifProfessionnelRepository extends JpaRepository<TarifProfessionnel, String> {
    List<TarifProfessionnel> findByProfessionnelIdAndActifTrue(String professionnelId);
    boolean existsByProfessionnelIdAndTitre(String professionnelId, String titre);
}
