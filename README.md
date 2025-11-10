# Java WebSocket Chat Bot

A comprehensive pure Java implementation of a chat bot system using WebSockets and multithreading. This project demonstrates advanced networking concepts, concurrent programming, and WebSocket protocol implementation without external dependencies.

## 🚀 Features

- **WebSocket Communication**: Full WebSocket protocol implementation from scratch
- **Multithreading**: Advanced thread pool management for concurrent connections
- **Chat Bot Intelligence**: Smart bot with pattern recognition and contextual responses  
- **User Management**: Complete session management and user tracking
- **Message Broadcasting**: Real-time message distribution to all connected clients
- **Command System**: Rich command interface with multiple bot interactions
- **Web Client**: Beautiful HTML/JavaScript client for browser-based chat
- **Console Client**: Full-featured Java console client for testing
- **Thread Safety**: Proper concurrent programming with thread-safe collections

## Project Structure

```
src/
├── main/
│   ├── server/
│   │   ├── ChatServer.java           # Main WebSocket server
│   │   ├── ClientHandler.java        # Individual client connection handler
│   │   ├── ChatBot.java             # Bot logic and responses
│   │   └── UserManager.java         # User session management
│   ├── client/
│   │   └── ChatClient.java          # Test client for connecting to server
│   ├── model/
│   │   ├── User.java               # User model
│   │   ├── Message.java            # Message model
│   │   └── Command.java            # Command model
│   └── util/
│       ├── WebSocketUtil.java      # WebSocket utility functions
│       └── ThreadPoolManager.java  # Thread pool management
└── test/
    └── ChatServerTest.java         # Basic tests
```

## 🏃‍♂️ Quick Start

### Method 1: Using Make (Recommended)
```bash
# Compile the project
make compile

# Start the server (default port 8080)
make server

# In another terminal, start a client
make client

# Run tests
make test
```

### Method 2: Using Shell Scripts
```bash
# Compile and start server
./compile.sh
./start-server.sh [port]

# Start client in another terminal
./start-client.sh [host] [port]
```

### Method 3: Manual Commands
```bash
# Compile
javac -d build src/main/**/*.java

# Run server
java -cp build main.server.ChatServer [port]

# Run client  
java -cp build main.client.ChatClient [host] [port]
```

### Method 4: Web Client
1. Start the server with any method above
2. Open `web-client.html` in your browser
3. Enter username and start chatting!

## WebSocket Protocol

The server runs on `ws://localhost:8080/chat`

### Message Format
```json
{
  "type": "message|command|join|leave",
  "username": "string",
  "content": "string",
  "timestamp": "long"
}
```

### Available Commands
- `/help` - Show available commands
- `/users` - List connected users
- `/time` - Get current server time
- `/bot <message>` - Talk directly to the bot
- `/quit` - Disconnect from server

## 🧪 Testing Multithreading

### Concurrent Users Test
```bash
# Terminal 1: Start server
make server

# Terminal 2-5: Start multiple clients
make client  # Repeat in different terminals
```

### Web Browser Test
1. Start server: `make server`
2. Open `web-client.html` in multiple browser tabs
3. Connect with different usernames
4. Send messages simultaneously from different tabs

## 🔧 Technologies Used

- **Pure Java**: No external dependencies, showcasing core Java capabilities
- **Socket Programming**: Raw socket implementation with proper connection handling
- **WebSocket Protocol**: Complete RFC 6455 WebSocket implementation
- **Multithreading**: ExecutorService thread pools and concurrent collections
- **Design Patterns**: Singleton, Observer, and Factory patterns
- **HTML/JavaScript**: Modern web client with WebSocket API integration

## 🏗️ Architecture

The system uses a multi-layered architecture:

- **Presentation Layer**: Web client (HTML/JS) and Console client (Java)
- **Application Layer**: ChatServer, ClientHandler, ChatBot
- **Business Logic**: Command processing, User management, Message routing  
- **Data Layer**: In-memory storage with thread-safe collections
- **Infrastructure**: Thread pools, WebSocket utilities, Configuration management

## 📊 Performance Features

- **Concurrent Connections**: Supports up to 100 simultaneous users
- **Thread Pool Management**: Separate pools for connections and message processing
- **Memory Efficient**: Proper cleanup of disconnected clients
- **Configurable**: Adjustable settings via configuration file
- **Scalable**: Architecture supports horizontal scaling

## 📚 Additional Resources

- **USAGE.md**: Comprehensive usage guide and testing scenarios
- **config.properties**: Server configuration options
- **Makefile**: Build automation and common tasks
- **web-client.html**: Feature-rich web interface
