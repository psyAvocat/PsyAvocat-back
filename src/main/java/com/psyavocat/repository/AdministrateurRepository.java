package com.psyavocat.repository;

import com.psyavocat.entity.Administrateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministrateurRepository extends JpaRepository<Administrateur, String> {
    Optional<Administrateur> findByEmail(String email);
    boolean existsByEmail(String email);
}

