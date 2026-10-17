package Conversational_AI.Database_Integration.repository;

import Conversational_AI.Database_Integration.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUsernameOrderByTimestampAsc(String username);
}