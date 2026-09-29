package com.psyavocat.repository;

import com.psyavocat.entity.Domaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DomaineRepository extends JpaRepository<Domaine, String> {
    Optional<Domaine> findByNom(String nom);
    boolean existsByNom(String nom);
}
