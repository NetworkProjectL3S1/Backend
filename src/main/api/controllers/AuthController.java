package main.api.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import main.api.ApiResponse;
import main.model.User;
import main.util.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication Controller
 * Handles user registration, login, and token verification
 */
public class AuthController implements HttpHandler {
    private final DatabaseManager dbManager;

    public AuthController() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        System.out.println("[AuthController] " + method + " " + path);

        try {
            if ("OPTIONS".equals(method)) {
                ApiResponse.sendJson(exchange, 200, "{}");
                return;
            }

            if (path.endsWith("/register")) {
                handleRegister(exchange);
            } else if (path.endsWith("/login")) {
                handleLogin(exchange);
            } else if (path.endsWith("/verify")) {
                handleVerifyToken(exchange);
            } else if (path.endsWith("/update-profile")) {
                handleUpdateProfile(exchange);
            } else {
                ApiResponse.sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            System.err.println("[AuthController] Error: " + e.getMessage());
            e.printStackTrace();
            ApiResponse.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Handle user registration
     */
    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            ApiResponse.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String requestBody = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

            Map<String, String> json = parseJson(requestBody);
            String username = json.getOrDefault("username", "").trim();
            String password = json.getOrDefault("password", "").trim();
            String email = json.getOrDefault("email", "").trim();
            String role = json.getOrDefault("role", "BUYER").trim().toUpperCase();

            // Validation
            if (username.isEmpty() || password.isEmpty()) {
                ApiResponse.sendError(exchange, 400, "Username and password are required");
                return;
            }

            if (password.length() < 6) {
                ApiResponse.sendError(exchange, 400, "Password must be at least 6 characters");
                return;
            }

            // Validate role
            if (!role.equals("BUYER") && !role.equals("SELLER")) {
                role = "BUYER"; // Default to BUYER if invalid
            }

            // Check if username already exists
            if (dbManager.userExists(username)) {
                ApiResponse.sendError(exchange, 409, "Username already exists");
                return;
            }

            // Create and save user
            String token = generateToken();
            boolean success = dbManager.registerUser(username, password, email, role, token);

            if (success) {
                String response = String.format(
                    "{\"success\":true,\"message\":\"User registered successfully\",\"token\":\"%s\",\"user\":{\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}}",
                    token, escapeJson(username), escapeJson(email), role
                );
                ApiResponse.sendJson(exchange, 201, response);
                System.out.println("[AuthController] User registered: " + username);
            } else {
                ApiResponse.sendError(exchange, 500, "Failed to register user");
            }

        } catch (Exception e) {
            System.err.println("[AuthController] Registration error: " + e.getMessage());
            ApiResponse.sendError(exchange, 500, "Registration failed: " + e.getMessage());
        }
    }

    /**
     * Handle user login
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            ApiResponse.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String requestBody = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

            Map<String, String> json = parseJson(requestBody);
            String username = json.getOrDefault("username", "").trim();
            String password = json.getOrDefault("password", "").trim();

            // Validation
            if (username.isEmpty() || password.isEmpty()) {
                ApiResponse.sendError(exchange, 400, "Username and password are required");
                return;
            }

            // Verify credentials
            User user = dbManager.authenticateUser(username, password);

            if (user != null) {
                // Generate new token for this session
                String token = generateToken();
                dbManager.updateUserToken(username, token);

                String response = String.format(
                    "{\"success\":true,\"message\":\"Login successful\",\"token\":\"%s\",\"user\":{\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}}",
                    token, escapeJson(user.getUsername()), escapeJson(user.getEmail()), user.getRole()
                );
                ApiResponse.sendJson(exchange, 200, response);
                System.out.println("[AuthController] User logged in: " + username);
            } else {
                ApiResponse.sendError(exchange, 401, "Invalid username or password");
            }

        } catch (Exception e) {
            System.err.println("[AuthController] Login error: " + e.getMessage());
            ApiResponse.sendError(exchange, 500, "Login failed: " + e.getMessage());
        }
    }

    /**
     * Handle token verification
     */
    private void handleVerifyToken(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        try {
            String token = null;
            
            // Support both GET (with Authorization header) and POST (with token in body)
            if ("GET".equals(method)) {
                // Extract token from Authorization header
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            } else if ("POST".equals(method)) {
                // Extract token from request body
                String requestBody = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

                Map<String, String> json = parseJson(requestBody);
                token = json.getOrDefault("token", "").trim();
            } else {
                ApiResponse.sendError(exchange, 405, "Method not allowed");
                return;
            }

            if (token == null || token.isEmpty()) {
                ApiResponse.sendError(exchange, 400, "Token is required");
                return;
            }

            User user = dbManager.getUserByToken(token);

            if (user != null) {
                String response = String.format(
                    "{\"valid\":true,\"user\":{\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}}",
                    escapeJson(user.getUsername()), escapeJson(user.getEmail()), user.getRole()
                );
                ApiResponse.sendJson(exchange, 200, response);
            } else {
                String response = "{\"valid\":false}";
                ApiResponse.sendJson(exchange, 200, response);
            }

        } catch (Exception e) {
            System.err.println("[AuthController] Token verification error: " + e.getMessage());
            ApiResponse.sendError(exchange, 500, "Verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle profile update
     */
    private void handleUpdateProfile(HttpExchange exchange) throws IOException {
        if (!"PUT".equals(exchange.getRequestMethod())) {
            ApiResponse.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // Extract token from Authorization header
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token == null || token.isEmpty()) {
                ApiResponse.sendError(exchange, 401, "Unauthorized - No token provided");
                return;
            }

            // Verify user by token
            User user = dbManager.getUserByToken(token);
            if (user == null) {
                ApiResponse.sendError(exchange, 401, "Unauthorized - Invalid token");
                return;
            }

            String requestBody = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

            Map<String, String> json = parseJson(requestBody);
            String newEmail = json.getOrDefault("email", "").trim();
            String currentPassword = json.getOrDefault("currentPassword", "").trim();
            String newPassword = json.getOrDefault("newPassword", "").trim();

            // Validate email if provided
            if (!newEmail.isEmpty() && !newEmail.contains("@")) {
                ApiResponse.sendError(exchange, 400, "Invalid email format");
                return;
            }

            // Handle password change if requested
            if (!newPassword.isEmpty()) {
                if (currentPassword.isEmpty()) {
                    ApiResponse.sendError(exchange, 400, "Current password is required to change password");
                    return;
                }

                if (newPassword.length() < 6) {
                    ApiResponse.sendError(exchange, 400, "New password must be at least 6 characters");
                    return;
                }

                // Verify current password
                User verifiedUser = dbManager.authenticateUser(user.getUsername(), currentPassword);
                if (verifiedUser == null) {
                    ApiResponse.sendError(exchange, 401, "Current password is incorrect");
                    return;
                }

                // Update password
                boolean passwordUpdated = dbManager.updateUserPassword(user.getUsername(), newPassword);
                if (!passwordUpdated) {
                    ApiResponse.sendError(exchange, 500, "Failed to update password");
                    return;
                }
            }

            // Update email
            boolean emailUpdated = true;
            if (!newEmail.isEmpty() && !newEmail.equals(user.getEmail())) {
                emailUpdated = dbManager.updateUserEmail(user.getUsername(), newEmail);
            }

            if (emailUpdated) {
                String response = String.format(
                    "{\"success\":true,\"message\":\"Profile updated successfully\",\"user\":{\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}}",
                    escapeJson(user.getUsername()), escapeJson(newEmail.isEmpty() ? user.getEmail() : newEmail), user.getRole()
                );
                ApiResponse.sendJson(exchange, 200, response);
                System.out.println("[AuthController] Profile updated for user: " + user.getUsername());
            } else {
                ApiResponse.sendError(exchange, 500, "Failed to update profile");
            }

        } catch (Exception e) {
            System.err.println("[AuthController] Profile update error: " + e.getMessage());
            ApiResponse.sendError(exchange, 500, "Profile update failed: " + e.getMessage());
        }
    }

    /**
     * Generate a unique authentication token
     */
    private String generateToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }
    
    /**
     * Simple JSON parser for basic key-value pairs
     */
    private Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isEmpty()) return map;
        
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");
                map.put(key, value);
            }
        }
        return map;
    }
    
    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
