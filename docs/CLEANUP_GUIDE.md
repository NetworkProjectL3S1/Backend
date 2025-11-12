# Project Cleanup and Migration Guide

## Overview
This document outlines the migration from file-based storage to SQL database storage and lists files that can be removed.

## What Changed

### 1. Storage System Migration
- **Before**: File-based storage using `AuctionFileStorage.java`
- **After**: SQL database storage using `DatabaseManager.java`
- All auction and bid data now stored in SQLite database (`data/auction_system.db`)

### 2. New REST API
- Added REST API server for React frontend integration
- API runs on port 8081 by default
- Endpoints for auction creation, listing, and bid placement
- Full CORS support for cross-origin requests

## Files to Remove

The following files are no longer needed and can be safely removed:

### 1. File Storage (Replaced by Database)
- `src/main/util/AuctionFileStorage.java` - Replaced by DatabaseManager
- `data/auctions/` directory - Old auction storage (if exists)
- `data/auctions/index.dat` - Old index file (if exists)

### 2. Old Client Files (If using REST API)
- `src/main/client/AuctionCreatorClient.java` - Can use React frontend instead
- `web-client.html` - Replaced by proper React application
- `start-client.sh` - Not needed for API-based frontend

### 3. Test Data Files
- `data/auctions/*.dat` - Old serialized auction files
- `data/auctions/*.txt` - Old text export files

### 4. Unnecessary Scripts (Optional)
If you're only using the API:
- `start-client.sh` - Old client launcher
- `start-server.sh` - Can be replaced with `start-api-server.sh`

## Migration Steps

### Step 1: Backup Existing Data (If Any)
```bash
# Backup old auction data
mkdir -p backup
cp -r data/auctions backup/ 2>/dev/null || true
```

### Step 2: Clean Up Old Files
```bash
# Remove old file storage system
rm -f src/main/util/AuctionFileStorage.java

# Remove old auction data files (after backup)
rm -rf data/auctions/

# Remove old client files (if using REST API only)
rm -f src/main/client/AuctionCreatorClient.java
rm -f web-client.html
rm -f start-client.sh
```

### Step 3: Recompile Project
```bash
# Download dependencies
./download-dependencies.sh

# Compile the project
./compile.sh
```

### Step 4: Start the API Server
```bash
# Start the REST API server
./start-api-server.sh
```

## What to Keep

### Essential Backend Files
- `src/main/api/` - REST API server and controllers
- `src/main/server/` - Core server logic (AuctionServer, AuctionManager, etc.)
- `src/main/model/` - Data models (Auction, Bid, etc.)
- `src/main/util/DatabaseManager.java` - Database operations
- `src/main/util/ConfigManager.java` - Configuration management
- `config.properties` - Server configuration

### Build and Run Scripts
- `compile.sh` - Build script
- `download-dependencies.sh` - Dependency management
- `start-api-server.sh` - API server launcher
- `Makefile` - Build automation

### Documentation
- `README.md` - Main documentation
- `API_DOCUMENTATION.md` - API reference
- `DATABASE_GUIDE.md` - Database usage guide
- `MIGRATION_GUIDE.md` - This file

### React Integration
- `react-example/` - React component examples

## Database Structure

The new database has two main tables:

### Auctions Table
```sql
CREATE TABLE auctions (
    auction_id TEXT PRIMARY KEY,
    item_name TEXT NOT NULL,
    item_description TEXT,
    seller_id TEXT NOT NULL,
    base_price REAL NOT NULL,
    current_highest_bid REAL NOT NULL,
    current_highest_bidder TEXT,
    created_time INTEGER NOT NULL,
    end_time INTEGER NOT NULL,
    duration INTEGER NOT NULL,
    status TEXT NOT NULL,
    category TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

### Bids Table
```sql
CREATE TABLE bids (
    bid_id INTEGER PRIMARY KEY AUTOINCREMENT,
    auction_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    amount REAL NOT NULL,
    timestamp INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id)
)
```

## Benefits of Migration

1. **Better Performance**: SQL queries are faster than file I/O
2. **Data Integrity**: Foreign key constraints ensure data consistency
3. **Easier Querying**: Complex queries using SQL instead of manual filtering
4. **Concurrent Access**: Better handling of concurrent operations
5. **Backup**: Simple database file backup
6. **REST API**: Modern API for frontend integration

## Testing the Migration

### 1. Test API Endpoints
```bash
# Health check
curl http://localhost:8081/api/health

# Create a test auction
curl -X POST http://localhost:8081/api/auctions/create \
  -H "Content-Type: application/json" \
  -d '{
    "itemName": "Test Item",
    "itemDescription": "Test Description",
    "sellerId": "testuser",
    "basePrice": 100,
    "duration": 60,
    "category": "test"
  }'

# List all auctions
curl http://localhost:8081/api/auctions/list
```

### 2. Check Database
```bash
# Install SQLite (if not already installed)
# macOS: brew install sqlite
# Ubuntu: sudo apt-get install sqlite3

# Open database
sqlite3 data/auction_system.db

# Query auctions
SELECT * FROM auctions;

# Query bids
SELECT * FROM bids;

# Exit
.exit
```

## Rollback (If Needed)

If you need to rollback to the old system:

1. Restore `AuctionFileStorage.java` from git
2. Update `AuctionManager.java` to use file storage
3. Restore backup data
4. Recompile

```bash
git checkout HEAD -- src/main/util/AuctionFileStorage.java
git checkout HEAD -- src/main/server/AuctionManager.java
cp -r backup/auctions data/
./compile.sh
```

## Next Steps

1. **Frontend Development**: Build React frontend using the API
2. **Authentication**: Add user authentication to the API
3. **Real-time Updates**: Add WebSocket support for live bid updates
4. **Deployment**: Deploy to production server
5. **Monitoring**: Add logging and monitoring

## Support

For issues or questions:
1. Check API_DOCUMENTATION.md for API details
2. Check DATABASE_GUIDE.md for database operations
3. Review error logs in terminal output
4. Check database file: `data/auction_system.db`
