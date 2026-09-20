package com.sqlassistant.model.dto;

public class ConfirmRequest {

    private String queryId;
    private Long userId;
    private Boolean confirm; // true to execute, false to cancel/reject

    public ConfirmRequest() {}

    public ConfirmRequest(String queryId, Long userId, Boolean confirm) {
        this.queryId = queryId;
        this.userId = userId;
        this.confirm = confirm;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getConfirm() {
        return confirm;
    }

    public void setConfirm(Boolean confirm) {
        this.confirm = confirm;
    }
}
