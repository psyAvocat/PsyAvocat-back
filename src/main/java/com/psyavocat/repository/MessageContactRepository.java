package com.psyavocat.repository;

import com.psyavocat.entity.MessageContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageContactRepository extends JpaRepository<MessageContact, String> {

    List<MessageContact> findByConversationIdOrderByDateEnvoiAsc(String conversationId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(m) FROM MessageContact m JOIN m.conversation c JOIN c.participants p WHERE p.id = :userId AND m.expediteur.id != :userId AND (m.lu = false OR m.lu IS NULL)")
    long countUnreadMessagesForUser(@org.springframework.data.repository.query.Param("userId") String userId);
}
