package com.psyavocat.repository;

import com.psyavocat.entity.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, String> {

    List<Questionnaire> findByActifTrue();

    Optional<Questionnaire> findByType(String type);

    Optional<Questionnaire> findByTypeAndActifTrue(String type);

    boolean existsByType(String type);

    Optional<Questionnaire> findByCode(String code);

    boolean existsByCode(String code);
}
