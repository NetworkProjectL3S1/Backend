package main.api;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for handling API responses
 */
public class ApiResponse {
    
    /**
     * Send JSON response with CORS headers
     */
    public static void sendJson(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * Send success response
     */
    public static void sendSuccess(HttpExchange exchange, Object data) throws IOException {
        String json = String.format("{\"success\":true,\"data\":%s}", toJson(data));
        sendJson(exchange, 200, json);
    }
    
    /**
     * Send error response
     */
    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = String.format("{\"success\":false,\"error\":\"%s\"}", escapeJson(message));
        sendJson(exchange, statusCode, json);
    }
    
    /**
     * Send created response (201)
     */
    public static void sendCreated(HttpExchange exchange, Object data) throws IOException {
        String json = String.format("{\"success\":true,\"data\":%s}", toJson(data));
        sendJson(exchange, 201, json);
    }
    
    /**
     * Simple JSON conversion for common types
     */
    private static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        // For complex objects, assume they have toString() or are already JSON
        return obj.toString();
    }
    
    /**
     * Escape JSON special characters
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
