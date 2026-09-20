package com.sqlassistant.model.dto;

import com.sqlassistant.model.Role;

public class UserDto {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private Long departmentId;

    public UserDto() {}

    public UserDto(Long id, String username, String fullName, String email, Role role, Long departmentId) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.departmentId = departmentId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
