package com.distributed.banking.controller;

import com.distributed.banking.model.User;
import com.distributed.banking.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> req) {
        try {
            String username = (String) req.get("username");
            String password = (String) req.get("password");
            String fullName = (String) req.get("fullName");
            String email = (String) req.get("email");
            Long branchId = Long.valueOf(req.get("branchId").toString());

            User user = authService.registerCustomer(username, password, fullName, email, branchId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Customer account registered successfully.",
                    "userId", user.getId(),
                    "username", user.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Registration error: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        try {
            String username = req.get("username");
            String password = req.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Username and password are required."));
            }

            Map<String, Object> authData = authService.login(username, password);
            return ResponseEntity.ok(authData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Authentication error: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = extractToken(authHeader);
            User user = authService.validateToken(token);
            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "fullName", user.getFullName(),
                    "email", user.getEmail(),
                    "role", user.getRole()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully."));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader;
    }
}
