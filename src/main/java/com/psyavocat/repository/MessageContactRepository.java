package com.psyavocat.repository;

import com.psyavocat.entity.MessageContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageContactRepository extends JpaRepository<MessageContact, String> {

    List<MessageContact> findByConversationIdOrderByDateEnvoiAsc(String conversationId);
}
