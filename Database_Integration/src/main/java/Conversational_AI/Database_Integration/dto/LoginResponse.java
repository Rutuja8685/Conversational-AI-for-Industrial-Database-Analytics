
package Conversational_AI.Database_Integration.dto;

public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String message;

    public LoginResponse(Long userId, String username, String role, String message) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getMessage() { return message; }
}