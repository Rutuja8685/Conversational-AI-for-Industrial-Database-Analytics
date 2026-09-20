package com.sqlassistant.controller;

import com.sqlassistant.model.User;
import com.sqlassistant.service.ChatSessionService;
import com.sqlassistant.model.ChatSession;
import com.sqlassistant.model.ChatMessage;
import java.util.Map;
import com.sqlassistant.model.dto.ConfirmRequest;
import com.sqlassistant.model.dto.QueryRequest;
import com.sqlassistant.model.dto.QueryResponse;
import com.sqlassistant.model.dto.UserDto;
import com.sqlassistant.repository.UserRepository;
import com.sqlassistant.service.QueryExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatSqlController {

    private final QueryExecutionService queryExecutionService;
    private final UserRepository userRepository;
    private final ChatSessionService chatSessionService;

    public ChatSqlController(QueryExecutionService queryExecutionService, UserRepository userRepository, ChatSessionService chatSessionService) {
        this.queryExecutionService = queryExecutionService;
        this.userRepository = userRepository;
        this.chatSessionService = chatSessionService;
    }

    /**
     * Process Natural Language Prompt -> SQL -> Execution / Pending Guardrail
     * POST /api/chat/query
     */
    @PostMapping("/chat/query")
    public ResponseEntity<QueryResponse> processQuery(@RequestBody QueryRequest request) {
        // Ensure a session exists; create if missing
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            // Create a new session; title will be generated from the first prompt later
            ChatSession session = chatSessionService.createSession(request.getUserId() != null ? request.getUserId().toString() : "1");
            sessionId = session.getId();
        }
        QueryResponse response = queryExecutionService.processQuery(request);
        response.setSessionId(sessionId);
        // Persist the chat message linked to the session
        chatSessionService.saveMessage(sessionId, response);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirm or Reject a Pending Write Query (UPDATE / DELETE)
     * POST /api/chat/confirm
     */
    @PostMapping("/chat/confirm")
    public ResponseEntity<QueryResponse> confirmQuery(@RequestBody ConfirmRequest request) {
        QueryResponse response = queryExecutionService.confirmQuery(request);
        // Update persisted message status based on confirmation outcome
        if (request.getQueryId() != null) {
            chatSessionService.updateMessageOnConfirm(request.getQueryId(), response);
        }
        if ("BLOCKED_RBAC".equals(response.getStatus())) {
            return ResponseEntity.status(403).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Alias endpoint for confirmation: POST /api/sql/confirm
     */
    @PostMapping("/sql/confirm")
    public ResponseEntity<QueryResponse> confirmSql(@RequestBody ConfirmRequest request) {
        return confirmQuery(request);
    }

    /**
     * Helper endpoint to fetch system users & roles for easy switching in client UI
     * GET /api/users
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(), u.getDepartmentId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ----- Session Management Endpoints -----

    @PostMapping("/chat/session/new")
    public ResponseEntity<Map<String, String>> createSession(@RequestParam(required = false) String userId) {
        ChatSession session = chatSessionService.createSession(userId != null ? userId : "1");
        return ResponseEntity.ok(Map.of("sessionId", session.getId()));
    }

    @GetMapping("/chat/sessions")
    public ResponseEntity<List<ChatSession>> getUserSessions(@RequestParam(required = false) String userId) {
        List<ChatSession> sessions = chatSessionService.getSessionsForUser(userId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/chat/session/{sessionId}/messages")
    public ResponseEntity<List<ChatMessage>> getSessionMessages(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatSessionService.getSessionMessages(sessionId);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/chat/session/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        boolean deleted = chatSessionService.deleteSession(sessionId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
