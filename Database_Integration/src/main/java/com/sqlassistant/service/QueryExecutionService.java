package com.sqlassistant.service;

import com.sqlassistant.model.PendingQuery;
import com.sqlassistant.model.Role;
import com.sqlassistant.model.User;
import com.sqlassistant.model.dto.*;
import com.sqlassistant.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QueryExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutionService.class);

    private final SchemaService schemaService;
    private final LlmService llmService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    // In-memory concurrent store for pending queries awaiting user confirmation
    private final Map<String, PendingQuery> pendingStore = new ConcurrentHashMap<>();

    public QueryExecutionService(SchemaService schemaService, LlmService llmService, 
                                 UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.schemaService = schemaService;
        this.llmService = llmService;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Process natural language query request with Intent Routing (CHITCHAT vs DATABASE_QUERY).
     */
    public QueryResponse processQuery(QueryRequest request) {
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return QueryResponse.error("Prompt cannot be empty");
        }

        User user = resolveUser(request.getUserId());

        // 1. Intent Classification / Routing
        LlmService.IntentType intent = llmService.classifyIntent(request.getPrompt());

        if (intent == LlmService.IntentType.GREETING_OR_CHITCHAT) {
            String chitchatReply = llmService.generateChitchatResponse(request.getPrompt(), user);
            QueryResponse response = new QueryResponse();
            response.setPrompt(request.getPrompt());
            response.setSql(null); // No SQL executed for general chit-chat
            response.setOperationType("CHITCHAT");
            response.setStatus("EXECUTED");
            response.setSummary(chitchatReply);
            response.setExplanation("Conversational AI Assistant Greeting");
            response.setUserRole(user.getRole().name());
            return response;
        }

        // 2. Database Query Execution Pipeline
        SchemaSummary schemaSummary = schemaService.getDynamicSchema();
        LlmService.LlmSqlResult llmResult = llmService.translateToSql(request.getPrompt(), schemaSummary);
        String sql = llmResult.getSql();
        String opType = llmResult.getOperationType();

        QueryResponse response = new QueryResponse();
        response.setPrompt(request.getPrompt());
        response.setSql(sql);
        response.setOperationType(opType);
        response.setExplanation(llmResult.getExplanation());
        response.setAffectedTables(llmResult.getAffectedTables());
        response.setUserRole(user.getRole().name());

        if ("SELECT".equalsIgnoreCase(opType)) {
            // Safe operation: Execute directly
            try {
                long startTime = System.currentTimeMillis();
                List<Map<String, Object>> rowsMap = jdbcTemplate.queryForList(sql);
                long executionTime = System.currentTimeMillis() - startTime;

                List<String> columns = new ArrayList<>();
                List<List<Object>> rowValues = new ArrayList<>();

                if (!rowsMap.isEmpty()) {
                    columns.addAll(rowsMap.get(0).keySet());
                    for (Map<String, Object> row : rowsMap) {
                        rowValues.add(new ArrayList<>(row.values()));
                    }
                }

                QueryResultData data = new QueryResultData(columns, rowValues, rowValues.size(), executionTime);
                String summary = llmService.generateDataSummary(request.getPrompt(), sql, columns, rowValues);

                response.setStatus("EXECUTED");
                response.setResult(data);
                response.setSummary(summary);

            } catch (Exception e) {
                logger.error("SQL Execution failed for query: {}", sql, e);
                response.setStatus("ERROR");
                response.setError("Failed to execute generated SELECT query: " + e.getMessage());
            }
        } else {
            // Write operation (UPDATE, DELETE, INSERT): Guarded execution required
            String queryId = UUID.randomUUID().toString();
            PendingQuery pending = new PendingQuery(
                    queryId, 
                    request.getPrompt(), 
                    sql, 
                    opType, 
                    llmResult.getExplanation(), 
                    llmResult.getAffectedTables(), 
                    user.getId(), 
                    user.getUsername()
            );

            pendingStore.put(queryId, pending);

            response.setQueryId(queryId);
            response.setStatus("NEEDS_CONFIRMATION");
            response.setSummary("Safety Guardrail Active: Write query (" + opType + ") generated. Explicit approval required.");
        }

        return response;
    }

    /**
     * Executes or rejects a pending query following RBAC security check.
     */
    public QueryResponse confirmQuery(ConfirmRequest request) {
        if (request.getQueryId() == null || !pendingStore.containsKey(request.getQueryId())) {
            return QueryResponse.error("Pending query ID not found or already processed");
        }

        PendingQuery pending = pendingStore.get(request.getQueryId());
        User user = resolveUser(request.getUserId());

        QueryResponse response = new QueryResponse();
        response.setQueryId(pending.getQueryId());
        response.setPrompt(pending.getPrompt());
        response.setSql(pending.getSql());
        response.setOperationType(pending.getOperationType());
        response.setExplanation(pending.getExplanation());
        response.setAffectedTables(pending.getAffectedTables());
        response.setUserRole(user.getRole().name());

        // RBAC Verification Rule: Block VIEWER or ENGINEER roles from approving UPDATE/DELETE
        if (user.getRole() != Role.ADMIN) {
            response.setStatus("BLOCKED_RBAC");
            response.setError("RBAC Violation: User '" + user.getUsername() + "' with role [" + user.getRole() + 
                    "] is blocked from approving write operations. Only ADMIN role can confirm UPDATE/DELETE.");
            return response;
        }

        if (Boolean.FALSE.equals(request.getConfirm())) {
            pending.setStatus("REJECTED");
            pendingStore.remove(pending.getQueryId());
            response.setStatus("REJECTED");
            response.setSummary("Query execution was cancelled by user " + user.getUsername());
            return response;
        }

        // Confirmed by ADMIN: Execute write query directly
        try {
            long startTime = System.currentTimeMillis();
            int rowsAffected = jdbcTemplate.update(pending.getSql());
            long executionTime = System.currentTimeMillis() - startTime;

            pending.setStatus("EXECUTED");
            pendingStore.remove(pending.getQueryId());

            QueryResultData data = new QueryResultData(rowsAffected, executionTime);
            response.setStatus("EXECUTED");
            response.setResult(data);
            response.setSummary("Successfully executed " + pending.getOperationType() + " query. " + rowsAffected + " row(s) affected.");

        } catch (Exception e) {
            logger.error("Failed to execute confirmed SQL write query: {}", pending.getSql(), e);
            response.setStatus("ERROR");
            response.setError("Execution error: " + e.getMessage());
        }

        return response;
    }

    public List<PendingQuery> getPendingQueries() {
        return new ArrayList<>(pendingStore.values());
    }

    private User resolveUser(Long userId) {
        if (userId != null) {
            Optional<User> uOpt = userRepository.findById(userId);
            if (uOpt.isPresent()) return uOpt.get();
        }
        // Fallback to default user 1 (sarah_admin) if not specified
        return userRepository.findById(1L).orElseGet(() -> new User(1L, "sarah_admin", "admin123", "Sarah Jenkins", "sarah@factory.io", Role.ADMIN, 1L));
    }
}
