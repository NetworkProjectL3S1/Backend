package main.util;

import main.model.Message;
import java.util.List;

/**
 * Storage manager that automatically switches between MySQL and file storage
 * based on availability and configuration
 */
public class StorageManager {
    private static StorageManager instance;
    private final boolean useMySQLStorage;
    private final MySQLMessageStorage mysqlStorage;
    private final MessageStorage fileStorage;
    
    private StorageManager() {
        // Try to initialize MySQL storage first
        this.mysqlStorage = MySQLMessageStorage.getInstance();
        this.fileStorage = MessageStorage.getInstance();
        
        // Check if MySQL is available and connected
        this.useMySQLStorage = mysqlStorage.isConnected();
        
        if (useMySQLStorage) {
            System.out.println("[STORAGE] Using MySQL database storage 🗄️");
        } else {
            System.out.println("[STORAGE] MySQL not available, using file storage 📁");
        }
    }
    
    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }
    
    /**
     * Save a message using the available storage system
     */
    public void saveMessage(Message message) {
        if (useMySQLStorage && mysqlStorage.isConnected()) {
            mysqlStorage.saveMessage(message);
        } else {
            fileStorage.saveMessage(message);
        }
    }
    
    /**
     * Get recent messages using the available storage system
     */
    public List<Message> getRecentMessages(int count) {
        if (useMySQLStorage && mysqlStorage.isConnected()) {
            return mysqlStorage.getRecentMessages(count);
        } else {
            return fileStorage.getRecentMessages(count);
        }
    }
    
    /**
     * Get messages by user using the available storage system
     */
    public List<Message> getMessagesByUser(String username) {
        if (useMySQLStorage && mysqlStorage.isConnected()) {
            return mysqlStorage.getMessagesByUser(username);
        } else {
            return fileStorage.getMessagesByUser(username);
        }
    }
    
    /**
     * Get today's message count using the available storage system
     */
    public int getTodaysMessageCount() {
        if (useMySQLStorage && mysqlStorage.isConnected()) {
            return mysqlStorage.getTodaysMessageCount();
        } else {
            return fileStorage.getTodaysMessageCount();
        }
    }
    
    /**
     * Get storage statistics using the available storage system
     */
    public String getStorageStats() {
        if (useMySQLStorage && mysqlStorage.isConnected()) {
            return "🗄️ MySQL: " + mysqlStorage.getStorageStats();
        } else {
            return "📁 File: " + fileStorage.getStorageStats();
        }
    }
    
    /**
     * Check which storage system is being used
     */
    public String getStorageType() {
        return useMySQLStorage && mysqlStorage.isConnected() ? "MySQL" : "File";
    }
    
    /**
     * Check if MySQL is available
     */
    public boolean isMySQLAvailable() {
        return useMySQLStorage && mysqlStorage.isConnected();
    }
    
    /**
     * Close storage connections
     */
    public void close() {
        if (mysqlStorage != null) {
            mysqlStorage.close();
        }
    }
}
