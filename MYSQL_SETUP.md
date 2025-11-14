# MySQL Database Setup Guide

This guide helps you set up MySQL database storage for the chat application.

## 🚀 Quick Setup

### 1. Install MySQL (if not already installed)

**macOS:**
```bash
brew install mysql
brew services start mysql
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
```

**Windows:**
Download from [MySQL Downloads](https://dev.mysql.com/downloads/)

### 2. Download MySQL JDBC Driver
```bash
./setup-mysql.sh
```

### 3. Setup Database
```bash
./setup-database.sh
```

### 4. Configure Database Connection
Edit `config.properties`:
```properties
# Database settings
db.host=localhost
db.port=3306
db.name=chatapp
db.username=root
db.password=your_mysql_password
```

### 5. Run the Server
```bash
./start-server.sh
```

## 🗄️ Database Schema

### Messages Table
```sql
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50) NOT NULL DEFAULT 'message',
    username VARCHAR(100),
    content TEXT NOT NULL,
    target_user VARCHAR(100) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### Users Table (Optional)
```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    first_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    message_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);
```

## 📊 New Chat Commands

With MySQL storage, you get new commands:

- `/history [count]` - View recent messages
- `/stats` - View database statistics  
- `/search [username]` - Find messages by user

## 🔧 Troubleshooting

### Connection Issues
```bash
# Test MySQL connection
mysql -u root -p

# Check if MySQL is running
brew services list | grep mysql  # macOS
sudo systemctl status mysql      # Linux
```

### Permission Issues
```bash
# Grant permissions to root user
mysql -u root -p
GRANT ALL PRIVILEGES ON chatapp.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

### Reset Database
```bash
mysql -u root -p
DROP DATABASE chatapp;
./setup-database.sh
```

## 📈 Performance Tips

1. **Indexes**: The schema includes optimized indexes for common queries
2. **Connection Pool**: Configured for multiple concurrent users
3. **UTF8MB4**: Full Unicode support for emojis and international characters
4. **Daily Partitioning**: Messages are efficiently queried by date

## 🔄 Fallback Mode

If MySQL is not available, the system automatically falls back to file storage:
- Messages saved to `chat_logs/messages_YYYY-MM-DD.json`
- Same functionality with file-based persistence
- No configuration changes needed

## 📚 Advanced Configuration

### Custom Database Settings
```properties
db.connection.pool.size=10
db.connection.timeout=30000
db.host=your-mysql-host
db.port=3306
```

### SSL Connection (Production)
```properties
db.ssl=true
db.ssl.cert=/path/to/cert.pem
```

---

🎉 **Your chat messages are now safely stored in MySQL!** 🗄️
