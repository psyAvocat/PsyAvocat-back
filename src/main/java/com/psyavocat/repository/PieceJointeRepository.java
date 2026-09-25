package com.psyavocat.repository;

import com.psyavocat.entity.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, String> {

    List<PieceJointe> findByDossierId(String dossierId);
}
