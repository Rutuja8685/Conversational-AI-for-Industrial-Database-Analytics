package com.sqlassistant.controller;

import com.sqlassistant.model.User;
import com.sqlassistant.model.dto.AuthResponse;
import com.sqlassistant.model.dto.LoginRequest;
import com.sqlassistant.model.dto.UserDto;
import com.sqlassistant.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Username and password required", null));
        }

        Optional<User> uOpt = userRepository.findByUsername(request.getUsername().trim());
        if (uOpt.isPresent()) {
            User user = uOpt.get();
            if (user.getPassword().equals(request.getPassword().trim())) {
                UserDto dto = new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole(), user.getDepartmentId());
                return ResponseEntity.ok(new AuthResponse(true, "Login successful", dto));
            }
        }

        return ResponseEntity.status(401).body(new AuthResponse(false, "Invalid username or password", null));
    }
}
