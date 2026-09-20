package com.sqlassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlassistant.model.ChatMessage;
import com.sqlassistant.model.ChatSession;
import com.sqlassistant.model.dto.QueryResponse;
import com.sqlassistant.repository.ChatMessageRepository;
import com.sqlassistant.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatSessionService {

    private static final Logger logger = LoggerFactory.getLogger(ChatSessionService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public ChatSessionService(ChatSessionRepository sessionRepository, 
                              ChatMessageRepository messageRepository, 
                              ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    public ChatSession createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        String safeUserId = (userId != null && !userId.trim().isEmpty()) ? userId : "1";
        ChatSession session = new ChatSession(sessionId, "New Chat", safeUserId, LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public ChatSession getOrCreateSession(String sessionId, String prompt, String userId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            Optional<ChatSession> opt = sessionRepository.findById(sessionId);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        // Create new session with auto-generated title from prompt
        String safeUserId = (userId != null && !userId.trim().isEmpty()) ? userId : "1";
        String newSessionId = (sessionId != null && !sessionId.trim().isEmpty()) ? sessionId : UUID.randomUUID().toString();
        String title = generateTitleFromPrompt(prompt);
        ChatSession session = new ChatSession(newSessionId, title, safeUserId, LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public List<ChatSession> getSessionsForUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return sessionRepository.findAllByOrderByCreatedAtDesc();
        }
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ChatMessage> getSessionMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    @Transactional
    public boolean deleteSession(String sessionId) {
        if (sessionRepository.existsById(sessionId)) {
            messageRepository.deleteBySessionId(sessionId);
            sessionRepository.deleteById(sessionId);
            return true;
        }
        return false;
    }

    public ChatMessage saveMessage(String sessionId, QueryResponse response) {
        try {
            String resultJson = null;
            if (response.getResult() != null) {
                resultJson = objectMapper.writeValueAsString(response.getResult());
            }

            ChatMessage message = new ChatMessage(
                    sessionId,
                    response.getPrompt(),
                    response.getSummary(),
                    response.getSql(),
                    resultJson,
                    response.getOperationType(),
                    response.getStatus(),
                    response.getQueryId(),
                    LocalDateTime.now()
            );

            return messageRepository.save(message);
        } catch (Exception e) {
            logger.error("Failed to save chat message for session {}", sessionId, e);
            return null;
        }
    }

    public void updateMessageOnConfirm(String queryId, QueryResponse response) {
        try {
            Optional<ChatMessage> opt = messageRepository.findByQueryId(queryId);
            if (opt.isPresent()) {
                ChatMessage msg = opt.get();
                msg.setStatus(response.getStatus());
                msg.setAiSummary(response.getSummary());
                if (response.getResult() != null) {
                    msg.setResultJson(objectMapper.writeValueAsString(response.getResult()));
                }
                messageRepository.save(msg);
            }
        } catch (Exception e) {
            logger.error("Failed to update chat message on confirmation for queryId {}", queryId, e);
        }
    }

    public String generateTitleFromPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "New Chat";
        }
        String clean = prompt.trim().replaceAll("\\s+", " ");
        String[] words = clean.split(" ");
        if (words.length <= 4) {
            return clean;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append(" ");
            sb.append(words[i]);
        }
        sb.append("...");
        return sb.toString();
    }
}
