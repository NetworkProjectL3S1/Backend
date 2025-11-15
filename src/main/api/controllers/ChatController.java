package main.api.controllers;

import com.sun.net.httpserver.HttpExchange;
import main.api.ApiResponse;
import main.model.ChatMessage;
import main.util.DatabaseManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Chat Messages
 */
public class ChatController {
    private final DatabaseManager dbManager;

    public ChatController() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Get chat messages for a specific auction
     * GET /api/chat/messages?auctionId=xxx
     */
    public void getMessages(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String auctionId = extractQueryParam(query, "auctionId");

        if (auctionId == null || auctionId.isEmpty()) {
            ApiResponse.sendError(exchange, 400, "Missing auctionId parameter");
            return;
        }

        List<ChatMessage> messages = dbManager.getChatMessagesByAuction(auctionId);
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (i > 0) json.append(",");
            json.append("{");
            json.append("\"messageId\":").append(msg.getMessageId()).append(",");
            json.append("\"auctionId\":\"").append(escapeJson(msg.getAuctionId())).append("\",");
            json.append("\"username\":\"").append(escapeJson(msg.getSenderUsername())).append("\",");
            json.append("\"senderUsername\":\"").append(escapeJson(msg.getSenderUsername())).append("\",");
            json.append("\"recipientUsername\":\"").append(escapeJson(msg.getRecipientUsername())).append("\",");
            json.append("\"content\":\"").append(escapeJson(msg.getContent())).append("\",");
            json.append("\"timestamp\":").append(msg.getTimestamp()).append(",");
            json.append("\"isRead\":").append(msg.isRead());
            json.append("}");
        }
        json.append("]");

        ApiResponse.sendSuccessRaw(exchange, json.toString());
    }

    /**
     * Get buyers who have chatted for seller's auctions
     * GET /api/chat/buyers?sellerId=xxx
     */
    public void getBuyers(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String sellerId = extractQueryParam(query, "sellerId");

        if (sellerId == null || sellerId.isEmpty()) {
            ApiResponse.sendError(exchange, 400, "Missing sellerId parameter");
            return;
        }

        Map<String, List<String>> auctionBuyers = dbManager.getBuyersBySeller(sellerId);
        
        StringBuilder json = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, List<String>> entry : auctionBuyers.entrySet()) {
            if (count++ > 0) json.append(",");
            json.append("\"").append(escapeJson(entry.getKey())).append("\":[");
            List<String> buyers = entry.getValue();
            for (int i = 0; i < buyers.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escapeJson(buyers.get(i))).append("\"");
            }
            json.append("]");
        }
        json.append("}");

        ApiResponse.sendSuccessRaw(exchange, json.toString());
    }

    /**
     * Mark messages as read
     * POST /api/chat/mark-read
     * Body: { "auctionId": "xxx", "username": "xxx" }
     */
    public void markAsRead(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes());
        
        // Simple JSON parsing
        String auctionId = extractJsonValue(requestBody, "auctionId");
        String username = extractJsonValue(requestBody, "username");

        if (auctionId == null || auctionId.isEmpty() || username == null || username.isEmpty()) {
            ApiResponse.sendError(exchange, 400, "Missing auctionId or username");
            return;
        }

        boolean success = dbManager.markMessagesAsRead(auctionId, username);
        
        if (success) {
            ApiResponse.sendSuccessRaw(exchange, "{\"message\":\"Messages marked as read\"}");
        } else {
            ApiResponse.sendError(exchange, 500, "Failed to mark messages as read");
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
     * Simple JSON value extraction
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
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
