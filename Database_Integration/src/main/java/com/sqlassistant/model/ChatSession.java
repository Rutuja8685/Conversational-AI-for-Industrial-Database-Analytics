package com.sqlassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ChatSession() {}

    public ChatSession(String id, String title, String userId, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
