# Real-Time Auction Platform with REST API

A comprehensive Java implementation of a real-time auction platform with REST API support, SQL database storage, WebSocket communication, multithreading, and Java NIO. Perfect for React frontend integration and modern web applications.

## 🚀 New Features (v2.0)

### ✨ REST API Server
- **Modern HTTP API**: Full REST API for auction and bid management
- **React Integration**: Ready-to-use API endpoints for React frontends
- **CORS Support**: Cross-origin resource sharing enabled
- **JSON Responses**: Standard JSON format for all responses
- **HTTP Methods**: GET, POST support for all operations

### 💾 SQL Database Storage
- **SQLite Database**: Migrated from file storage to SQL database
- **Better Performance**: Faster queries and concurrent access
- **Data Integrity**: Foreign key constraints and transactions
- **Easy Backup**: Simple database file backup and restore
- **Indexing**: Optimized queries with proper indexing

### 🎯 Core Features

#### Auction System
- **Auction Creation**: Create auctions via REST API or server
- **Real-Time Updates**: Live auction status and bid updates
- **Category Management**: Filter auctions by category
- **Time Management**: Automatic expiration tracking
- **Thread-Safe**: Concurrent operations handled properly

#### Bidding System
- **Place Bids**: Submit bids via REST API
- **Bid History**: Track all bids for each auction
- **Validation**: Automatic bid amount validation
- **Real-Time Notifications**: Instant bid updates

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- macOS, Linux, or Windows with WSL
- curl (for testing)

### Installation

1. **Download dependencies**
```bash
./download-dependencies.sh
```

2. **Compile the project**
```bash
./compile.sh
```

### Running the API Server

**Start the REST API server:**
```bash
./start-api-server.sh
```

The API will be available at: `http://localhost:8081/api/`

### Testing the API

**Create an auction:**
```bash
curl -X POST http://localhost:8081/api/auctions/create \
  -H "Content-Type: application/json" \
  -d '{
    "itemName": "Vintage Watch",
    "itemDescription": "Beautiful 1960s watch",
    "sellerId": "user123",
    "basePrice": 100,
    "duration": 60,
    "category": "collectibles"
  }'
```

**List all auctions:**
```bash
curl http://localhost:8081/api/auctions/list
```

**Place a bid:**
```bash
curl -X POST http://localhost:8081/api/bids/place \
  -H "Content-Type: application/json" \
  -d '{
    "auctionId": "auction-1234",
    "userId": "user456",
    "amount": 150
  }'
```

## 📚 Documentation

- **[docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)**: Complete API reference
- **[docs/CLEANUP_GUIDE.md](docs/CLEANUP_GUIDE.md)**: Migration and cleanup guide
- **[docs/UPGRADE_SUMMARY.md](docs/UPGRADE_SUMMARY.md)**: Upgrade details and summary
- **[docs/DATABASE_GUIDE.md](docs/DATABASE_GUIDE.md)**: Database operations guide
- **[docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md)**: Testing procedures
- **[react-example/](react-example/)**: React component examples

## 🔧 React Integration

See [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md) and [react-example/AuctionComponents.jsx](react-example/AuctionComponents.jsx) for complete integration examples.

## 💾 Database

- **File**: `data/auction_system.db`
- **Type**: SQLite
- **Backups**: `data/backups/`

---

**Version**: 2.0.0  
**Last Updated**: November 2025
