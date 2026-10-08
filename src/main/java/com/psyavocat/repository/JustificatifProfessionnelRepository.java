package com.psyavocat.repository;

import com.psyavocat.entity.JustificatifProfessionnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JustificatifProfessionnelRepository extends JpaRepository<JustificatifProfessionnel, String> {
    List<JustificatifProfessionnel> findByProfessionnelId(String professionnelId);
}
