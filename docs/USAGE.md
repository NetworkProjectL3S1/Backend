# Java WebSocket Chat Bot - Usage Guide

## Quick Start

### 1. Compile the Project
```bash
make compile
# or
./compile.sh
```

### 2. Start the Server
```bash
make server
# or
./start-server.sh
# or manually:
java -cp build main.server.ChatServer [port]
```

### 3. Connect Clients

**Option A: Java Console Client**
```bash
make client
# or
./start-client.sh
# or manually:
java -cp build main.client.ChatClient [host] [port]
```

**Option B: Web Browser Client**
1. Open `web-client.html` in your browser
2. Make sure the server is running on localhost:8080
3. Enter a username and click "Connect"

## Features Demonstrated

### 1. **Multithreading**
- **Thread Pool Management**: Uses `ExecutorService` with cached thread pool for client connections
- **Concurrent Message Processing**: Separate thread pool for message processing
- **Thread Safety**: Uses `ConcurrentHashMap` and thread-safe collections
- **Example**: Connect multiple clients simultaneously to see concurrent handling

### 2. **WebSocket Implementation**
- **Pure Java WebSocket**: No external libraries, implements WebSocket protocol from scratch
- **Handshake Process**: Proper WebSocket handshake with key generation and validation
- **Frame Encoding/Decoding**: Handles WebSocket frame format for text messages
- **Connection Management**: Proper connection lifecycle management

### 3. **Chat Bot Features**
- **Automated Responses**: Bot responds to certain keywords and patterns
- **Command Processing**: Handles various commands like `/help`, `/users`, `/time`
- **Random Response**: Bot occasionally responds to regular messages (10% probability)
- **Personality**: Includes jokes, math operations, and contextual responses

## Available Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/help` | Show available commands | `/help` |
| `/users` | List connected users | `/users` |
| `/time` | Get current server time | `/time` |
| `/bot <message>` | Chat directly with bot | `/bot hello there` |
| `/pm <user> <message>` | Send private message | `/pm alice how are you?` |
| `/quit` | Disconnect from server | `/quit` |

## Testing Multithreading

### Test Scenario 1: Multiple Concurrent Users
1. Start the server: `make server`
2. Open 3-5 terminal windows
3. In each window run: `make client`
4. Enter different usernames for each client
5. Send messages from different clients simultaneously
6. Observe server handling concurrent connections

### Test Scenario 2: Stress Testing
1. Use the web client to connect multiple browser tabs
2. Send rapid messages from different clients
3. Use bot commands extensively
4. Monitor server console for thread pool activity

### Test Scenario 3: Connection Management
1. Connect multiple clients
2. Disconnect some clients abruptly (Ctrl+C)
3. Observe server cleanup and user management
4. Verify other clients continue working normally

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Client    │    │  Java Client    │    │  Java Client    │
│  (HTML/JS)      │    │    Console      │    │    Console      │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────┴──────────────┐
                    │      ChatServer            │
                    │  (Main WebSocket Server)   │
                    └─────────────┬──────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
 ┌────────▼────────┐   ┌─────────▼─────────┐   ┌────────▼────────┐
 │  ClientHandler  │   │  ClientHandler    │   │  ClientHandler  │
 │   (Thread 1)    │   │   (Thread 2)      │   │   (Thread 3)    │
 └─────────────────┘   └───────────────────┘   └─────────────────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────▼──────────────┐
                    │       ChatBot              │
                    │   (Message Processing)     │
                    └────────────────────────────┘
```

## Key Multithreading Components

1. **ThreadPoolManager**: Manages two thread pools
   - `clientHandlerPool`: Handles client connections (cached thread pool)
   - `messageProcessorPool`: Processes messages (fixed thread pool of 10)

2. **Concurrent Data Structures**:
   - `ConcurrentHashMap<String, User>` for user storage
   - `ConcurrentHashMap<String, ClientHandler>` for connection mapping
   - `ConcurrentHashMap<ClientHandler, Boolean>` for active connections

3. **Thread Safety Mechanisms**:
   - Synchronized methods in UserManager
   - Atomic operations with `AtomicBoolean`
   - Thread-safe message broadcasting

## WebSocket Protocol Implementation

1. **Handshake Process**:
   - Client sends HTTP upgrade request
   - Server validates WebSocket key
   - Server generates accept key using SHA-1 + Base64
   - Server responds with 101 Switching Protocols

2. **Frame Format**:
   - Supports text frames (opcode 0x1)
   - Handles masked/unmasked frames
   - Supports close frames (opcode 0x8)
   - Handles ping/pong frames for keep-alive

3. **Message Processing**:
   - Decodes incoming WebSocket frames
   - Processes messages and commands
   - Encodes responses as WebSocket frames
   - Broadcasts to multiple clients

## Performance Considerations

- **Connection Limit**: Configurable maximum connections (default: 100)
- **Message Size**: Limited to 500 characters per message
- **Thread Pools**: Optimized for concurrent client handling
- **Memory Management**: Proper cleanup of disconnected clients
- **Bot Response Rate**: Configurable probability (default: 10%)

## Troubleshooting

### Common Issues

1. **"Address already in use"**
   - Another process is using port 8080
   - Kill the process or use a different port: `make server PORT=8081`

2. **Web client can't connect**
   - Ensure server is running on localhost:8080
   - Check browser developer console for WebSocket errors
   - Try refreshing the page

3. **Compilation errors**
   - Ensure you have Java JDK installed (Java 8 or higher)
   - Check JAVA_HOME environment variable
   - Run `make clean` then `make compile`

### Debug Mode

To enable more verbose logging, modify the server startup:
```bash
java -cp build -Djava.util.logging.config.file=logging.properties main.server.ChatServer
```

## Extension Ideas

1. **Add SSL/TLS support** for secure WebSocket (WSS)
2. **Implement chat rooms** for organized conversations
3. **Add file sharing capabilities** with binary frame support
4. **Create a persistence layer** to store chat history
5. **Implement user authentication** and authorization
6. **Add emoji and rich text support**
7. **Create mobile clients** using WebSocket libraries
8. **Add server clustering** for horizontal scaling

This project demonstrates core networking concepts, multithreading, and WebSocket protocol implementation in pure Java!
