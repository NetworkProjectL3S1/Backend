package main.api.controllers;

import com.sun.net.httpserver.HttpExchange;
import main.api.ApiResponse;
import main.model.Notification;
import main.util.DatabaseManager;

import java.io.IOException;
import java.util.List;

/**
 * REST API Controller for Notifications
 */
public class NotificationController {
    private final DatabaseManager dbManager;

    public NotificationController() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Get notifications for a user
     * GET /api/notifications?username=xxx
     */
    public void getNotifications(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String username = extractQueryParam(query, "username");

        if (username == null || username.isEmpty()) {
            ApiResponse.sendError(exchange, 400, "Missing username parameter");
            return;
        }

        List<Notification> notifications = dbManager.getNotifications(username);
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < notifications.size(); i++) {
            Notification notif = notifications.get(i);
            if (i > 0) json.append(",");
            json.append("{");
            json.append("\"notificationId\":").append(notif.getNotificationId()).append(",");
            json.append("\"username\":\"").append(escapeJson(notif.getUsername())).append("\",");
            json.append("\"type\":\"").append(escapeJson(notif.getType())).append("\",");
            json.append("\"title\":\"").append(escapeJson(notif.getTitle())).append("\",");
            json.append("\"message\":\"").append(escapeJson(notif.getMessage())).append("\",");
            json.append("\"auctionId\":").append(notif.getAuctionId() != null ? 
                "\"" + escapeJson(notif.getAuctionId()) + "\"" : "null").append(",");
            json.append("\"timestamp\":").append(notif.getTimestamp()).append(",");
            json.append("\"isRead\":").append(notif.isRead());
            json.append("}");
        }
        json.append("]");

        ApiResponse.sendSuccessRaw(exchange, json.toString());
    }

    /**
     * Get unread notification count
     * GET /api/notifications/unread-count?username=xxx
     */
    public void getUnreadCount(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String username = extractQueryParam(query, "username");

        if (username == null || username.isEmpty()) {
            ApiResponse.sendError(exchange, 400, "Missing username parameter");
            return;
        }

        int count = dbManager.getUnreadNotificationCount(username);
        String json = "{\"count\":" + count + "}";
        
        ApiResponse.sendSuccessRaw(exchange, json);
    }

    /**
     * Mark notification as read
     * POST /api/notifications/mark-read
     * Body: { "notificationId": 123 }
     */
    public void markAsRead(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes());
        
        String notificationIdStr = extractJsonValue(requestBody, "notificationId");

        if (notificationIdStr == null || notificationIdStr.isEmpty()) {
            ApiResponse.sendError(exchange, 400, "Missing notificationId");
            return;
        }

        try {
            long notificationId = Long.parseLong(notificationIdStr);
            boolean success = dbManager.markNotificationAsRead(notificationId);
            
            if (success) {
                ApiResponse.sendSuccessRaw(exchange, "{\"message\":\"Notification marked as read\"}");
            } else {
                ApiResponse.sendError(exchange, 500, "Failed to mark notification as read");
            }
        } catch (NumberFormatException e) {
            ApiResponse.sendError(exchange, 400, "Invalid notificationId format");
        }
    }

    /**
     * Mark all notifications as read for a user
     * POST /api/notifications/mark-all-read
     * Body: { "username": "xxx" }
     */
    public void markAllAsRead(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes());
        
        String username = extractJsonValue(requestBody, "username");

        if (username == null || username.isEmpty()) {
            ApiResponse.sendError(exchange, 400, "Missing username");
            return;
        }

        boolean success = dbManager.markAllNotificationsAsRead(username);
        
        if (success) {
            ApiResponse.sendSuccessRaw(exchange, "{\"message\":\"All notifications marked as read\"}");
        } else {
            ApiResponse.sendError(exchange, 500, "Failed to mark notifications as read");
        }
    }

    /**
     * Extract query parameter from query string
     */
    private String extractQueryParam(String query, String param) {
        if (query == null) return null;
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param)) {
                return keyValue[1];
            }
        }
        return null;
    }

    /**
     * Extract value from simple JSON string
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"?([^,\"\\}]+)\"?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * Escape JSON special characters
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
