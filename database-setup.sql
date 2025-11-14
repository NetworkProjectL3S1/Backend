-- MySQL Database Setup for Chat Application
-- Execute this script in MySQL to set up the database

-- Create database
CREATE DATABASE IF NOT EXISTS chatapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE chatapp;

-- Create messages table with optimized structure
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50) NOT NULL DEFAULT 'message',
    username VARCHAR(100),
    content TEXT NOT NULL,
    target_user VARCHAR(100) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for better query performance
    INDEX idx_username (username),
    INDEX idx_timestamp (timestamp),
    INDEX idx_type (type),
    INDEX idx_target_user (target_user),
    INDEX idx_date (DATE(timestamp)),
    
    -- Composite indexes for common query patterns
    INDEX idx_user_date (username, DATE(timestamp)),
    INDEX idx_type_date (type, DATE(timestamp))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create users table for additional user information (optional)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    first_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    message_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    
    INDEX idx_username (username),
    INDEX idx_last_seen (last_seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create a view for today's messages (for easier queries)
CREATE OR REPLACE VIEW todays_messages AS
SELECT 
    id,
    timestamp,
    type,
    username,
    content,
    target_user
FROM messages 
WHERE DATE(timestamp) = CURDATE()
ORDER BY timestamp ASC;

-- Create a view for message statistics
CREATE OR REPLACE VIEW message_stats AS
SELECT 
    COUNT(*) as total_messages,
    COUNT(DISTINCT username) as unique_users,
    COUNT(DISTINCT DATE(timestamp)) as active_days,
    MAX(timestamp) as last_message_time,
    MIN(timestamp) as first_message_time,
    AVG(LENGTH(content)) as avg_message_length
FROM messages;

-- Sample data insertion (optional - remove if not needed)
-- INSERT INTO messages (type, username, content) VALUES 
-- ('system', 'ChatBot', 'Welcome to the chat system!'),
-- ('message', 'admin', 'Server is now running with MySQL storage!');

-- Show table information
SHOW TABLES;
DESCRIBE messages;

-- Display current statistics
SELECT 'Database setup completed successfully!' as status;
SELECT * FROM message_stats;
