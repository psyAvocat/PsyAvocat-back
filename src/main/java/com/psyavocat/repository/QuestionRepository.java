package com.psyavocat.repository;

import com.psyavocat.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, String> {

    @Query("SELECT q FROM Question q WHERE q.questionnaire.id = :questionnaireId ORDER BY q.ordre ASC")
    List<Question> findByQuestionnaireIdOrderByOrdreAsc(@Param("questionnaireId") String questionnaireId);

    @Query("SELECT q FROM Question q WHERE q.questionnaire.id = :questionnaireId AND q.code = :code")
    Optional<Question> findByQuestionnaireIdAndCode(@Param("questionnaireId") String questionnaireId, @Param("code") String code);

    @Query("SELECT COUNT(q) > 0 FROM Question q WHERE q.questionnaire.id = :questionnaireId AND q.code = :code")
    boolean existsByQuestionnaireIdAndCode(@Param("questionnaireId") String questionnaireId, @Param("code") String code);
}
