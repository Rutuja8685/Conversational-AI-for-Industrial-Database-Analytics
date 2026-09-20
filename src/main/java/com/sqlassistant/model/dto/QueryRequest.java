package com.sqlassistant.model.dto;

public class QueryRequest {

    private String prompt;
    private Long userId;
    private String sessionId;

    public QueryRequest() {}

    public QueryRequest(String prompt, Long userId) {
        this.prompt = prompt;
        this.userId = userId;
    }

    public QueryRequest(String prompt, Long userId, String sessionId) {
        this.prompt = prompt;
        this.userId = userId;
        this.sessionId = sessionId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
