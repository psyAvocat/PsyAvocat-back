package com.psyavocat.repository;

import com.psyavocat.entity.DecisionAdministrative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionAdministrativeRepository extends JpaRepository<DecisionAdministrative, String> {

    List<DecisionAdministrative> findByCibleIdOrderByDateDecisionDesc(String cibleId);
}
