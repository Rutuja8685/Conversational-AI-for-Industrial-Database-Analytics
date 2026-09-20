package com.sqlassistant.repository;

import com.sqlassistant.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    Optional<ChatMessage> findByQueryId(String queryId);

    @Transactional
    void deleteBySessionId(String sessionId);
}
