package main.model;

/**
 * Represents a message in the chat system
 */
public class Message {
    public enum MessageType {
        MESSAGE, COMMAND, JOIN, LEAVE, SYSTEM, BOT_RESPONSE
    }
    
    private MessageType type;
    private String username;
    private String content;
    private long timestamp;
    private String targetUser; // for private messages
    
    public Message(MessageType type, String username, String content) {
        this.type = type;
        this.username = username;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Message(MessageType type, String username, String content, String targetUser) {
        this(type, username, content);
        this.targetUser = targetUser;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTargetUser() {
        return targetUser;
    }
    
    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }
    
    /**
     * Convert message to JSON-like string format
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"").append(type.toString().toLowerCase()).append("\",");
        json.append("\"username\":\"").append(username != null ? username : "").append("\",");
        json.append("\"content\":\"").append(content != null ? content.replace("\"", "\\\"") : "").append("\",");
        json.append("\"timestamp\":").append(timestamp);
        if (targetUser != null) {
            json.append(",\"targetUser\":\"").append(targetUser).append("\"");
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Parse a simple JSON-like string to create a Message object
     */
    public static Message fromJson(String json) {
        try {
            // Simple JSON parsing (for production, consider using a proper JSON library)
            String type = extractJsonValue(json, "type");
            String username = extractJsonValue(json, "username");
            String content = extractJsonValue(json, "content");
            String targetUser = extractJsonValue(json, "targetUser");
            
            MessageType messageType = MessageType.MESSAGE;
            if (type != null) {
                try {
                    messageType = MessageType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Default to MESSAGE if type is not recognized
                }
            }
            
            Message message = new Message(messageType, username, content);
            if (targetUser != null && !targetUser.isEmpty()) {
                message.setTargetUser(targetUser);
            }
            
            return message;
        } catch (Exception e) {
            // Return a system message if parsing fails
            return new Message(MessageType.SYSTEM, "system", "Invalid message format");
        }
    }
    
    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) {
            // Try without quotes for numbers
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            if (start == -1) return null;
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return end > start ? json.substring(start, end).trim() : null;
        }
        
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", username='" + username + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", targetUser='" + targetUser + '\'' +
                '}';
    }
}
