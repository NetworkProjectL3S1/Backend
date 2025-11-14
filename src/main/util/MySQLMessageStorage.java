package main.util;

import main.model.Message;
import java.sql.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MySQL-based message storage system
 * Stores messages in MySQL database with proper indexing and performance optimization
 */
public class MySQLMessageStorage {
    private static MySQLMessageStorage instance;
    private static final String DB_URL_TEMPLATE = "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConfigManager configManager;
    private Connection connection;
    private boolean isConnected = false;
    
    // SQL Queries
    private static final String CREATE_MESSAGES_TABLE = """
        CREATE TABLE IF NOT EXISTS messages (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            type VARCHAR(50) NOT NULL,
            username VARCHAR(100),
            content TEXT NOT NULL,
            target_user VARCHAR(100),
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_username (username),
            INDEX idx_timestamp (timestamp),
            INDEX idx_type (type),
            INDEX idx_target_user (target_user)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """;
    
    private static final String INSERT_MESSAGE = """
        INSERT INTO messages (timestamp, type, username, content, target_user) 
        VALUES (?, ?, ?, ?, ?)
    """;
    
    private static final String SELECT_RECENT_MESSAGES = """
        SELECT * FROM messages 
        WHERE DATE(timestamp) = CURDATE() 
        ORDER BY timestamp DESC 
        LIMIT ?
    """;
    
    private static final String SELECT_MESSAGES_BY_USER = """
        SELECT * FROM messages 
        WHERE username = ? AND DATE(timestamp) = CURDATE() 
        ORDER BY timestamp ASC
    """;
    
    private static final String SELECT_TODAYS_COUNT = """
        SELECT COUNT(*) FROM messages 
        WHERE DATE(timestamp) = CURDATE()
    """;
    
    private static final String SELECT_STATS = """
        SELECT 
            COUNT(*) as total_messages,
            COUNT(DISTINCT username) as unique_users,
            COUNT(DISTINCT DATE(timestamp)) as active_days,
            MAX(timestamp) as last_message_time
        FROM messages
    """;
    
    private MySQLMessageStorage() {
        this.configManager = ConfigManager.getInstance();
        initializeConnection();
        createTables();
    }
    
    public static synchronized MySQLMessageStorage getInstance() {
        if (instance == null) {
            instance = new MySQLMessageStorage();
        }
        return instance;
    }
    
    /**
     * Initialize database connection
     */
    private void initializeConnection() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            String host = configManager.getProperty("db.host", "localhost");
            int port = Integer.parseInt(configManager.getProperty("db.port", "3306"));
            String dbName = configManager.getProperty("db.name", "chatapp");
            String username = configManager.getProperty("db.username", "root");
            String password = configManager.getProperty("db.password", "");
            
            String dbUrl = String.format(DB_URL_TEMPLATE, host, port, dbName);
            
            System.out.println("[DB] Connecting to MySQL database: " + dbUrl);
            
            connection = DriverManager.getConnection(dbUrl, username, password);
            connection.setAutoCommit(true);
            
            isConnected = true;
            System.out.println("[DB] Successfully connected to MySQL database");
            
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] MySQL JDBC driver not found. Please add mysql-connector-java to classpath");
            System.err.println("[DB] Download from: https://dev.mysql.com/downloads/connector/j/");
            isConnected = false;
        } catch (SQLException e) {
            System.err.println("[DB] Failed to connect to MySQL database: " + e.getMessage());
            System.err.println("[DB] Please ensure MySQL is running and credentials are correct");
            isConnected = false;
        }
    }
    
    /**
     * Create necessary database tables
     */
    private void createTables() {
        if (!isConnected) {
            return;
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(CREATE_MESSAGES_TABLE);
            System.out.println("[DB] Messages table created/verified successfully");
        } catch (SQLException e) {
            System.err.println("[DB] Error creating tables: " + e.getMessage());
        }
    }
    
    /**
     * Save a message to database
     */
    public void saveMessage(Message message) {
        if (!isConnected) {
            System.err.println("[DB] Cannot save message - no database connection");
            return;
        }
        
        lock.writeLock().lock();
        try (PreparedStatement pstmt = connection.prepareStatement(INSERT_MESSAGE)) {
            
            pstmt.setTimestamp(1, new Timestamp(message.getTimestamp()));
            pstmt.setString(2, message.getType().toString().toLowerCase());
            pstmt.setString(3, message.getUsername());
            pstmt.setString(4, message.getContent());
            pstmt.setString(5, message.getTargetUser());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("[DB] Message saved: " + message.getUsername() + " - " + 
                    (message.getContent().length() > 50 ? message.getContent().substring(0, 50) + "..." : message.getContent()));
            }
            
        } catch (SQLException e) {
            System.err.println("[DB] Error saving message: " + e.getMessage());
            // Try to reconnect if connection is lost
            if (e.getErrorCode() == 0 || e.getMessage().contains("connection")) {
                reconnect();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get recent messages (last N messages from today)
     */
    public List<Message> getRecentMessages(int count) {
        if (!isConnected) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try (PreparedStatement pstmt = connection.prepareStatement(SELECT_RECENT_MESSAGES)) {
            
            pstmt.setInt(1, count);
            ResultSet rs = pstmt.executeQuery();
            
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                Message message = resultSetToMessage(rs);
                if (message != null) {
                    messages.add(message);
                }
            }
            
            // Reverse to get chronological order
            Collections.reverse(messages);
            return messages;
            
        } catch (SQLException e) {
            System.err.println("[DB] Error retrieving recent messages: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get messages by specific user for today
     */
    public List<Message> getMessagesByUser(String username) {
        if (!isConnected) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try (PreparedStatement pstmt = connection.prepareStatement(SELECT_MESSAGES_BY_USER)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                Message message = resultSetToMessage(rs);
                if (message != null) {
                    messages.add(message);
                }
            }
            
            return messages;
            
        } catch (SQLException e) {
            System.err.println("[DB] Error retrieving messages by user: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get today's message count
     */
    public int getTodaysMessageCount() {
        if (!isConnected) {
            return 0;
        }
        
        lock.readLock().lock();
        try (PreparedStatement pstmt = connection.prepareStatement(SELECT_TODAYS_COUNT)) {
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
            
        } catch (SQLException e) {
            System.err.println("[DB] Error getting today's message count: " + e.getMessage());
            return 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get comprehensive storage statistics
     */
    public String getStorageStats() {
        if (!isConnected) {
            return "Database connection not available";
        }
        
        lock.readLock().lock();
        try (PreparedStatement pstmt = connection.prepareStatement(SELECT_STATS)) {
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int totalMessages = rs.getInt("total_messages");
                int uniqueUsers = rs.getInt("unique_users");
                int activeDays = rs.getInt("active_days");
                Timestamp lastMessageTime = rs.getTimestamp("last_message_time");
                
                return String.format(
                    "📊 Database Stats - Total Messages: %d | Unique Users: %d | Active Days: %d | Last Message: %s",
                    totalMessages, uniqueUsers, activeDays, 
                    lastMessageTime != null ? lastMessageTime.toString() : "Never"
                );
            }
            return "No statistics available";
            
        } catch (SQLException e) {
            System.err.println("[DB] Error getting storage stats: " + e.getMessage());
            return "Error retrieving statistics: " + e.getMessage();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Convert ResultSet to Message object
     */
    private Message resultSetToMessage(ResultSet rs) throws SQLException {
        try {
            String typeStr = rs.getString("type");
            String username = rs.getString("username");
            String content = rs.getString("content");
            String targetUser = rs.getString("target_user");
            Timestamp timestamp = rs.getTimestamp("timestamp");
            
            Message.MessageType messageType = Message.MessageType.MESSAGE;
            if (typeStr != null) {
                try {
                    messageType = Message.MessageType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    messageType = Message.MessageType.MESSAGE;
                }
            }
            
            Message message = new Message(messageType, username, content, targetUser);
            if (timestamp != null) {
                message.setTimestamp(timestamp.getTime());
            }
            
            return message;
        } catch (Exception e) {
            System.err.println("[DB] Error converting ResultSet to Message: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Attempt to reconnect to database
     */
    private void reconnect() {
        System.out.println("[DB] Attempting to reconnect to database...");
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // Ignore close errors
        }
        
        isConnected = false;
        initializeConnection();
        
        if (isConnected) {
            createTables();
        }
    }
    
    /**
     * Check if database is connected
     */
    public boolean isConnected() {
        return isConnected && connection != null;
    }
    
    /**
     * Close database connection
     */
    public void close() {
        lock.writeLock().lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Database connection closed");
            }
            isConnected = false;
        } catch (SQLException e) {
            System.err.println("[DB] Error closing database connection: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Test database connection
     */
    public boolean testConnection() {
        if (!isConnected) {
            return false;
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 1");
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DB] Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
