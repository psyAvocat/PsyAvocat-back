package com.psyavocat.repository;

import com.psyavocat.entity.ConversationMasquee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMasqueeRepository extends JpaRepository<ConversationMasquee, String> {

    List<ConversationMasquee> findByUtilisateurId(String utilisateurId);

    Optional<ConversationMasquee> findByConversationIdAndUtilisateurId(String conversationId, String utilisateurId);

    List<ConversationMasquee> findByConversationId(String conversationId);
}
