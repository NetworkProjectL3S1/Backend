package main.server;

import main.model.User;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Simple HTTP server for user authentication (login and register only)
 * Provides REST API endpoints for frontend authentication
 */
public class AuthServer {
    private HttpServer server;
    private final UserManager userManager;
    private final int port;

    public AuthServer(int port) {
        this.port = port;
        this.userManager = UserManager.getInstance();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Create context for authentication endpoints
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/auth/verify", new VerifyHandler());
        
        // Set executor for handling requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        
        System.out.println("Auth Server started on port " + port);
        System.out.println("Endpoints:");
        System.out.println("  POST http://localhost:" + port + "/api/auth/login");
        System.out.println("  POST http://localhost:" + port + "/api/auth/register");
        System.out.println("  GET  http://localhost:" + port + "/api/auth/verify");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Auth Server stopped");
        }
    }

    /**
     * Handler for login requests
     * POST /api/auth/login
     * Body: {"username": "string", "password": "string"}
     */
    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCORSResponse(exchange, 200, "");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> request = parseJson(requestBody);
                
                String username = request.get("username");
                String password = request.get("password");

                if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Username and password are required\"}");
                    return;
                }

                User user = userManager.authenticateUser(username, password);
                
                if (user != null) {
                    // Generate a simple token
                    String token = generateToken(user);
                    
                    String response = String.format(
                        "{\"token\":\"%s\",\"user\":{\"username\":\"%s\",\"userId\":\"%s\",\"role\":\"%s\"}}",
                        token, user.getUsername(), user.getUserId(), user.getRole().name()
                    );
                    sendResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 401, "{\"error\":\"Invalid credentials or user already logged in\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
            }
        }
    }

    /**
     * Handler for registration requests
     * POST /api/auth/register
     * Body: {"username": "string", "password": "string", "email": "string", "role": "BUYER|SELLER"}
     */
    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCORSResponse(exchange, 200, "");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> request = parseJson(requestBody);
                
                String username = request.get("username");
                String password = request.get("password");
                String roleStr = request.get("role");

                if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Username and password are required\"}");
                    return;
                }

                // Parse role, default to BUYER if not provided or invalid
                User.Role role = User.Role.BUYER;
                if (roleStr != null) {
                    try {
                        role = User.Role.valueOf(roleStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Invalid role, use default
                    }
                }

                User user = userManager.registerUser(username, password, role);
                
                if (user != null) {
                    // Generate a simple token
                    String token = generateToken(user);
                    
                    String response = String.format(
                        "{\"token\":\"%s\",\"user\":{\"username\":\"%s\",\"userId\":\"%s\",\"role\":\"%s\"}}",
                        token, user.getUsername(), user.getUserId(), user.getRole().name()
                    );
                    sendResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 409, "{\"error\":\"Username already taken\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
            }
        }
    }

    /**
     * Handler for token verification
     * GET /api/auth/verify
     * Header: Authorization: Bearer <token>
     */
    private class VerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendCORSResponse(exchange, 200, "");
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendResponse(exchange, 401, "{\"valid\":false,\"error\":\"Missing or invalid authorization header\"}");
                    return;
                }

                String token = authHeader.substring(7); // Remove "Bearer "
                User user = validateToken(token);
                
                if (user != null) {
                    String response = String.format(
                        "{\"valid\":true,\"user\":{\"username\":\"%s\",\"userId\":\"%s\",\"role\":\"%s\"}}",
                        user.getUsername(), user.getUserId(), user.getRole().name()
                    );
                    sendResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 401, "{\"valid\":false,\"error\":\"Invalid token\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
            }
        }
    }

    /**
     * Simple JSON parser for request bodies
     */
    private Map<String, String> parseJson(String json) {
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) {
                return map;
            }
            
            int start = 0;
            boolean inQuotes = false;
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    parseKeyValue(json.substring(start, i), map);
                    start = i + 1;
                }
            }
            if (start < json.length()) {
                parseKeyValue(json.substring(start), map);
            }
        }
        return map;
    }
    
    private void parseKeyValue(String pair, Map<String, String> map) {
        pair = pair.trim();
        if (pair.isEmpty()) return;
        
        int colonIndex = -1;
        boolean inQuotes = false;
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            if (c == '"' && (i == 0 || pair.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ':' && !inQuotes) {
                colonIndex = i;
                break;
            }
        }
        
        if (colonIndex > 0 && colonIndex < pair.length() - 1) {
            String key = pair.substring(0, colonIndex).trim().replaceAll("^\"|\"$", "");
            String value = pair.substring(colonIndex + 1).trim().replaceAll("^\"|\"$", "");
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }
    }

    /**
     * Generate a simple token for the user
     */
    private String generateToken(User user) {
        String data = user.getUsername() + ":" + user.getUserId() + ":" + System.currentTimeMillis();
        return java.util.Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate a token and return the associated user
     */
    private User validateToken(String token) {
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length >= 2) {
                String username = parts[0];
                return userManager.getPersistentUser(username);
            }
        } catch (Exception e) {
            // Invalid token format
        }
        return null;
    }

    /**
     * Send HTTP response with CORS headers
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    /**
     * Send CORS preflight response
     */
    private void sendCORSResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

    /**
     * Main method to start the authentication server
     */
    public static void main(String[] args) {
        int port = 8080;
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.err.println("Port must be between 1024 and 65535");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
        AuthServer authServer = new AuthServer(port);
        try {
            authServer.start();
            System.out.println("\nPress Ctrl+C to stop the server");
        } catch (IOException e) {
            System.err.println("Failed to start Auth Server: " + e.getMessage());
            System.exit(1);
        }
    }
}

