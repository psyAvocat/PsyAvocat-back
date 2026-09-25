package com.psyavocat.repository;

import com.psyavocat.entity.NoteSeance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteSeanceRepository extends JpaRepository<NoteSeance, String> {

    List<NoteSeance> findBySeanceIdOrderByDateCreationDesc(String seanceId);
}
