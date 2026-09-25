package com.psyavocat.repository;

import com.psyavocat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p WHERE p.id = :userId ORDER BY c.dateCreation DESC")
    List<Conversation> findByParticipantId(@Param("userId") String userId);
}
