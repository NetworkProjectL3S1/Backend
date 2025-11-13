package main.util;

import main.model.Message;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple file-based message storage system
 * Stores messages in JSON format with daily rotation
 */
public class MessageStorage {
    private static MessageStorage instance;
    private static final String STORAGE_DIR = "chat_logs";
    private static final String MESSAGE_FILE_PREFIX = "messages_";
    private static final String FILE_EXTENSION = ".json";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<Message> todaysMessages = new ArrayList<>();
    
    private MessageStorage() {
        createStorageDirectory();
        loadTodaysMessages();
    }
    
    public static synchronized MessageStorage getInstance() {
        if (instance == null) {
            instance = new MessageStorage();
        }
        return instance;
    }
    
    /**
     * Save a message to storage
     */
    public void saveMessage(Message message) {
        lock.writeLock().lock();
        try {
            // Add to in-memory cache
            todaysMessages.add(message);
            
            // Write to file
            appendMessageToFile(message);
            
            System.out.println("[STORAGE] Message saved: " + message.getUsername() + " - " + message.getContent());
        } catch (Exception e) {
            System.err.println("[STORAGE] Error saving message: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get messages for today
     */
    public List<Message> getTodaysMessages() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(todaysMessages);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get recent messages (last N messages)
     */
    public List<Message> getRecentMessages(int count) {
        lock.readLock().lock();
        try {
            int size = todaysMessages.size();
            if (size <= count) {
                return new ArrayList<>(todaysMessages);
            }
            return new ArrayList<>(todaysMessages.subList(size - count, size));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Search messages by user
     */
    public List<Message> getMessagesByUser(String username) {
        lock.readLock().lock();
        try {
            return todaysMessages.stream()
                    .filter(msg -> username.equals(msg.getUsername()))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get message count for today
     */
    public int getTodaysMessageCount() {
        lock.readLock().lock();
        try {
            return todaysMessages.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private void createStorageDirectory() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("[STORAGE] Created storage directory: " + STORAGE_DIR);
            } else {
                System.err.println("[STORAGE] Failed to create storage directory: " + STORAGE_DIR);
            }
        }
    }
    
    private void loadTodaysMessages() {
        String today = dateFormat.format(new Date());
        String fileName = MESSAGE_FILE_PREFIX + today + FILE_EXTENSION;
        File file = new File(STORAGE_DIR, fileName);
        
        if (!file.exists()) {
            System.out.println("[STORAGE] No existing messages file for today: " + fileName);
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        Message message = parseMessageFromJson(line);
                        if (message != null) {
                            todaysMessages.add(message);
                            loadedCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("[STORAGE] Error parsing message: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("[STORAGE] Loaded " + loadedCount + " messages from " + fileName);
        } catch (IOException e) {
            System.err.println("[STORAGE] Error loading today's messages: " + e.getMessage());
        }
    }
    
    private void appendMessageToFile(Message message) {
        String today = dateFormat.format(new Date());
        String fileName = MESSAGE_FILE_PREFIX + today + FILE_EXTENSION;
        File file = new File(STORAGE_DIR, fileName);
        
        try (FileWriter writer = new FileWriter(file, true)) {
            String jsonMessage = messageToJson(message);
            writer.write(jsonMessage + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println("[STORAGE] Error writing message to file: " + e.getMessage());
        }
    }
    
    private String messageToJson(Message message) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":\"").append(timestampFormat.format(new Date(message.getTimestamp()))).append("\",");
        json.append("\"type\":\"").append(message.getType().toString().toLowerCase()).append("\",");
        json.append("\"username\":\"").append(escapeJson(message.getUsername() != null ? message.getUsername() : "")).append("\",");
        json.append("\"content\":\"").append(escapeJson(message.getContent() != null ? message.getContent() : "")).append("\"");
        
        if (message.getTargetUser() != null) {
            json.append(",\"targetUser\":\"").append(escapeJson(message.getTargetUser())).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }
    
    private Message parseMessageFromJson(String json) {
        try {
            // Simple JSON parsing
            String type = extractJsonValue(json, "type");
            String username = extractJsonValue(json, "username");
            String content = extractJsonValue(json, "content");
            String targetUser = extractJsonValue(json, "targetUser");
            String timestamp = extractJsonValue(json, "timestamp");
            
            Message.MessageType messageType = Message.MessageType.MESSAGE;
            if (type != null) {
                try {
                    messageType = Message.MessageType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    messageType = Message.MessageType.MESSAGE;
                }
            }
            
            Message message = new Message(messageType, username, content, targetUser);
            
            // Parse timestamp
            if (timestamp != null) {
                try {
                    Date date = timestampFormat.parse(timestamp);
                    message.setTimestamp(date.getTime());
                } catch (Exception e) {
                    // Keep current timestamp if parsing fails
                }
            }
            
            return message;
        } catch (Exception e) {
            System.err.println("[STORAGE] Error parsing JSON: " + e.getMessage());
            return null;
        }
    }
    
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return null;
        }
        
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return null;
        }
        
        return unescapeJson(json.substring(startIndex, endIndex));
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }
    
    /**
     * Get storage statistics
     */
    public String getStorageStats() {
        lock.readLock().lock();
        try {
            File dir = new File(STORAGE_DIR);
            File[] files = dir.listFiles((d, name) -> name.startsWith(MESSAGE_FILE_PREFIX) && name.endsWith(FILE_EXTENSION));
            int fileCount = files != null ? files.length : 0;
            
            return String.format("Storage Stats - Files: %d, Today's Messages: %d", 
                    fileCount, todaysMessages.size());
        } finally {
            lock.readLock().unlock();
        }
    }
}
