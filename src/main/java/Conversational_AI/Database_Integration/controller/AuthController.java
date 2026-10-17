package Conversational_AI.Database_Integration.controller;

import Conversational_AI.Database_Integration.dto.LoginRequest;
import Conversational_AI.Database_Integration.dto.LoginResponse;
import Conversational_AI.Database_Integration.entity.User;
import Conversational_AI.Database_Integration.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username") == null ? "" : body.get("username").trim();
        String password = body.get("password");

        if (username.length() < 3 || username.length() > 50) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username must be 3-50 characters"));
        }
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already taken"));
        }

        // Role is always set by the server, never by the client
        User user = new User(username, encoder.encode(password), "USER");
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> found = userRepository.findByUsername(loginRequest.getUsername());
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }

        User user = found.get();
        String stored = user.getPasswordHash();
        String entered = loginRequest.getPassword();
        boolean ok;

        if (stored != null && stored.startsWith("$2")) {
            ok = encoder.matches(entered, stored);
        } else {
            // Old plain-text password: accept it once, then upgrade it to a hash
            ok = stored != null && stored.equals(entered);
            if (ok) {
                user.setPasswordHash(encoder.encode(entered));
                userRepository.save(user);
            }
        }

        
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }

        return ResponseEntity.ok(new LoginResponse(user.getId(), user.getUsername(), user.getRole(), "Login successful"));
    }
}