# Real-Time Auction Platform (Backend)

A comprehensive Java implementation of a real-time auction platform with WebSocket communication, multithreading, and Java NIO. This project demonstrates advanced networking concepts, concurrent programming, file persistence, and client-server architecture.

## 🚀 Features

### Auction System (Module 2)

- **Auction Creation**: Sellers can create auctions with configurable parameters
- **Real-Time Broadcasting**: New auctions broadcast to all connected clients
- **File Persistence**: Auctions saved using Java I/O with automatic backup
- **Category Management**: Filter and browse auctions by category
- **Time-Based Management**: Automatic auction expiration and status tracking
- **Thread-Safe Operations**: Concurrent auction creation and bidding

### Chat System

- **WebSocket Communication**: Full WebSocket protocol implementation from scratch
- **Multithreading**: Advanced thread pool management for concurrent connections
- **Chat Bot Intelligence**: Smart bot with pattern recognition and contextual responses
- **User Management**: Complete session management and user tracking
- **Message Broadcasting**: Real-time message distribution to all connected clients
- **Command System**: Rich command interface with multiple bot interactions

### Real-Time Bidding

- **Live Bid Updates**: Instant bid notifications using Java NIO
- **Thread-Safe Bidding**: Synchronized bid placement with validation
- **Watcher System**: Clients can watch specific auctions for updates
- **Bid Broadcasting**: Real-time updates to all auction watchers

## Project Structure

```
src/
├── main/
│   ├── server/
│   │   ├── AuctionServer.java          # Main NIO auction server
│   │   ├── AuctionManager.java         # [MODULE 2] Auction lifecycle management
│   │   ├── AuctionClientHandler.java   # Individual client connection handler
│   │   ├── BidBroadcaster.java        # Real-time bid broadcasting
│   │   ├── ChatServer.java            # WebSocket chat server
│   │   ├── ClientHandler.java         # Chat client handler
│   │   ├── ChatBot.java              # Bot logic and responses
│   │   ├── UserManager.java          # User session management
│   │   └── ServerMain.java           # Server entry point
│   ├── client/
│   │   ├── AuctionCreatorClient.java  # [MODULE 2] Test client for auctions
│   │   └── ChatClient.java           # Test client for chat
│   ├── model/
│   │   ├── Auction.java              # [MODULE 2] Enhanced auction model
│   │   ├── Bid.java                  # Bid model
│   │   ├── User.java                 # User model
│   │   ├── Message.java              # Message model
│   │   └── Command.java              # Command model
│   └── util/
│       ├── AuctionFileStorage.java   # [MODULE 2] File I/O persistence
│       ├── WebSocketUtil.java        # WebSocket utility functions
│       ├── ThreadPoolManager.java    # Thread pool management
│       └── ConfigManager.java        # Configuration management
└── test/
    └── ChatServerTest.java           # Basic tests

data/
├── auctions/                         # Auction data storage
│   ├── *.dat                        # Binary auction files
│   ├── *.txt                        # Text exports
│   └── index.dat                    # Auction index
└── backups/                         # Backup storage
    └── auctions_backup_*.dat
```

## 🏃‍♂️ Quick Start

### Method 1: Using Shell Scripts (Recommended)

```bash
# Compile the project
./compile.sh

# Start the auction server
./start-server.sh

# In another terminal, start the auction creator client
./start-auction-creator.sh
# OR on Windows:
start-auction-creator.bat
```

### Method 2: Using Make

```bash
# Compile the project
make compile

# Start the server (default port 9999)
make server

# Start auction client
java -cp bin main.client.AuctionCreatorClient localhost 9999
```

### Method 3: Manual Commands

```bash
# Compile
javac -d bin src/main/**/*.java

# Run auction server
java -cp bin main.server.ServerMain

# Run auction creator client
java -cp bin main.client.AuctionCreatorClient localhost 9999
```

## 📝 Module 2: Auction Creation Usage

### Creating an Auction (Interactive Client)

```bash
# Start the client
java -cp bin main.client.AuctionCreatorClient localhost 9999

# Use the interactive menu
> create

Item Name: Vintage Camera
Description: Canon AE-1 from 1976
Seller ID: photoenthusiast
Base Price: $250.00
Duration (minutes): 90
Category: photography
```

### Creating an Auction (Programmatically)

```java
AuctionManager manager = new AuctionManager();

Auction auction = manager.createAuction(
    "Gaming Laptop",                    // itemName
    "High-end gaming laptop, RTX 4090", // description
    "seller123",                        // sellerId
    1500.00,                           // basePrice
    120,                               // duration (minutes)
    "electronics"                      // category
);
```

### Protocol Commands

#### CREATE_AUCTION

```
CREATE_AUCTION:itemName:description:sellerId:basePrice:durationMinutes:category
```

#### LIST_AUCTIONS

```
LIST_AUCTIONS              # List all active auctions
LIST_AUCTIONS:category     # List auctions by category
```

#### GET_AUCTION

```
GET_AUCTION:auctionId
```

#### WATCH (for bidding)

```
WATCH:auction-1
```

#### BID (place a bid)

```
BID:auction-1:amount
```

## 🔧 Module Implementation Status

| Module | Feature                | Status          | Developer       |
| ------ | ---------------------- | --------------- | --------------- |
| 1      | User Authentication    | 🟡 In Progress  | Member 1        |
| 2      | Auction Creation       | ✅ **Complete** | **Your Module** |
| 3      | Bidding System         | ✅ Complete     | Member 3        |
| 4      | Real-Time Broadcasting | ✅ Complete     | Member 4        |
| 5      | Chat System            | ✅ Complete     | Member 5        |

## � Documentation

- **[MODULE2_DOCUMENTATION.md](MODULE2_DOCUMENTATION.md)**: Comprehensive Module 2 documentation

  - Protocol specification
  - Usage examples
  - Integration guide
  - File persistence details
  - Testing scenarios

- **USAGE.md**: General usage guide and testing scenarios
- **config.properties**: Server configuration options
