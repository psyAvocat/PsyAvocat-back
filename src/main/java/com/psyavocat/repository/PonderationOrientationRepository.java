package com.psyavocat.repository;

import com.psyavocat.entity.PonderationOrientation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PonderationOrientationRepository extends JpaRepository<PonderationOrientation, String> {
    List<PonderationOrientation> findByReponseId(String reponseId);
    List<PonderationOrientation> findByReponseIdIn(List<String> reponseIds);
    boolean existsByReponseIdAndCategorieBesoinId(String reponseId, String categorieBesoinId);
    boolean existsByReponseIdAndSpecialiteId(String reponseId, String specialiteId);
}
