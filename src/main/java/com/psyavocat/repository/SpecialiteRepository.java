package com.psyavocat.repository;

import com.psyavocat.entity.Specialite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialiteRepository extends JpaRepository<Specialite, String> {

    Optional<Specialite> findByNomIgnoreCase(String nom);
}
