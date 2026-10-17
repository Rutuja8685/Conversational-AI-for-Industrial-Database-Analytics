// package Conversational_AI.Database_Integration.entity;

// public class ChatMessage {
    
// }
package Conversational_AI.Database_Integration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userPrompt;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String aiResponse;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public ChatMessage() {}

    public ChatMessage(String username, String userPrompt, String aiResponse, LocalDateTime timestamp) {
        this.username = username;
        this.userPrompt = userPrompt;
        this.aiResponse = aiResponse;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}