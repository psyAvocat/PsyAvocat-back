package com.psyavocat.repository;

import com.psyavocat.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'entité Utilisateur.
 * L'identifiant primaire `id` correspond directement au Firebase UID.
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, String> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
}
