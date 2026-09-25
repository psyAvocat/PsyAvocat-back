package com.psyavocat.repository;

import com.psyavocat.entity.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, String> {

    List<Dossier> findByJusticiableIdOrderByDateOuvertureDesc(String justiciableId);
}
