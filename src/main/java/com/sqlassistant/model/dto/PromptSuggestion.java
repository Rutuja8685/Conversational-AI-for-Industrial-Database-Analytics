package com.sqlassistant.model.dto;

public class PromptSuggestion {

    private String id;
    private String category;
    private String prompt;
    private String roleRequired; // ADMIN, ENGINEER, VIEWER (or ALL)
    private String operationType; // SELECT, UPDATE, DELETE

    public PromptSuggestion() {}

    public PromptSuggestion(String id, String category, String prompt, String roleRequired, String operationType) {
        this.id = id;
        this.category = category;
        this.prompt = prompt;
        this.roleRequired = roleRequired;
        this.operationType = operationType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getRoleRequired() { return roleRequired; }
    public void setRoleRequired(String roleRequired) { this.roleRequired = roleRequired; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
}
