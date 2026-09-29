package com.psyavocat.repository;

import com.psyavocat.entity.Reponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReponseRepository extends JpaRepository<Reponse, String> {

    @Query("SELECT r FROM Reponse r WHERE r.question.id = :questionId AND r.code = :code")
    Optional<Reponse> findByQuestionIdAndCode(@Param("questionId") String questionId, @Param("code") String code);

    @Query("SELECT COUNT(r) > 0 FROM Reponse r WHERE r.question.id = :questionId AND r.code = :code")
    boolean existsByQuestionIdAndCode(@Param("questionId") String questionId, @Param("code") String code);
}
