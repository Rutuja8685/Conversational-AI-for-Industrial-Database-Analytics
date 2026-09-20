package com.sqlassistant.model.dto;

import java.util.List;

public class QueryResponse {

    private String queryId;
    private String prompt;
    private String sql;
    private String operationType; // SELECT, UPDATE, DELETE, INSERT, INVALID, CHITCHAT
    private String status; // EXECUTED, PENDING_CONFIRMATION, REJECTED, BLOCKED_RBAC, ERROR
    private QueryResultData result;
    private String summary;
    private String explanation;
    private List<String> affectedTables;
    private String userRole;
    private String error;
    private String sessionId;

    public QueryResponse() {}

    public static QueryResponse error(String errorMsg) {
        QueryResponse res = new QueryResponse();
        res.setStatus("ERROR");
        res.setError(errorMsg);
        return res;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public QueryResultData getResult() {
        return result;
    }

    public void setResult(QueryResultData result) {
        this.result = result;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
