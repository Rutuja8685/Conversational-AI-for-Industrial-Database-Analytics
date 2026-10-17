package com.sqlassistant.model;

import java.time.LocalDateTime;
import java.util.List;

public class PendingQuery {

    private String queryId;
    private String prompt;
    private String sql;
    private String operationType;
    private String explanation;
    private List<String> affectedTables;
    private Long requestedByUserId;
    private String requestedByUsername;
    private String status; // PENDING_CONFIRMATION, EXECUTED, REJECTED, CANCELLED
    private LocalDateTime createdAt;

    public PendingQuery() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING_CONFIRMATION";
    }

    public PendingQuery(String queryId, String prompt, String sql, String operationType, 
                        String explanation, List<String> affectedTables, 
                        Long requestedByUserId, String requestedByUsername) {
        this.queryId = queryId;
        this.prompt = prompt;
        this.sql = sql;
        this.operationType = operationType;
        this.explanation = explanation;
        this.affectedTables = affectedTables;
        this.requestedByUserId = requestedByUserId;
        this.requestedByUsername = requestedByUsername;
        this.status = "PENDING_CONFIRMATION";
        this.createdAt = LocalDateTime.now();
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getAffectedTables() {
        return affectedTables;
    }

    public void setAffectedTables(List<String> affectedTables) {
        this.affectedTables = affectedTables;
    }

    public Long getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(Long requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
