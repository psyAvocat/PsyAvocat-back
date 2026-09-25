package com.psyavocat.repository;

import com.psyavocat.entity.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, String> {

    List<Questionnaire> findByActifTrue();
}
