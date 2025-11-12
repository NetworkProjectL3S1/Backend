# SQLite Database Integration Guide

## Overview

This project now uses SQLite for persistent storage of auctions and bids. The database provides reliable, file-based storage that persists data across server restarts.

## Database Schema

### Tables

#### auctions
Stores auction information:
- `auction_id` (TEXT, PRIMARY KEY) - Unique auction identifier
- `item_name` (TEXT) - Name of the item being auctioned
- `item_description` (TEXT) - Description of the item
- `seller_id` (TEXT) - ID of the seller
- `base_price` (REAL) - Starting price
- `current_highest_bid` (REAL) - Current highest bid amount
- `current_highest_bidder` (TEXT) - User ID of highest bidder
- `created_time` (INTEGER) - Timestamp when auction was created
- `end_time` (INTEGER) - Timestamp when auction ends
- `duration` (INTEGER) - Auction duration in milliseconds
- `status` (TEXT) - ACTIVE, CLOSED, or CANCELLED
- `category` (TEXT) - Auction category
- `created_at` (TIMESTAMP) - Database record creation time
- `updated_at` (TIMESTAMP) - Last update time

#### bids
Stores all bid history:
- `bid_id` (INTEGER, PRIMARY KEY, AUTOINCREMENT) - Unique bid identifier
- `auction_id` (TEXT, FOREIGN KEY) - Reference to auction
- `user_id` (TEXT) - ID of the bidder
- `amount` (REAL) - Bid amount
- `timestamp` (INTEGER) - When the bid was placed
- `created_at` (TIMESTAMP) - Database record creation time

### Indexes
- `idx_bids_auction_id` - Fast lookup of bids by auction
- `idx_bids_timestamp` - Efficient bid history queries

## Setup Instructions

### 1. Download Dependencies

Run the dependency download script to get the SQLite JDBC driver:

```bash
chmod +x download-dependencies.sh
./download-dependencies.sh
```

This will:
- Create a `lib` directory
- Download the SQLite JDBC driver (sqlite-jdbc-3.47.1.0.jar)

### 2. Compile the Project

```bash
chmod +x compile.sh
./compile.sh
```

The compile script will:
- Check for the SQLite JDBC driver
- Include it in the classpath
- Compile all Java source files

### 3. Run the Application

```bash
./start-server.sh [port]
```

The server will automatically:
- Create the `data` directory if it doesn't exist
- Initialize the SQLite database at `data/auction_system.db`
- Create the necessary tables on first run

## Using the DatabaseManager

### Singleton Instance

```java
DatabaseManager db = DatabaseManager.getInstance();
```

### Saving Data

#### Save an Auction
```java
Auction auction = new Auction(
    "AUC001",           // auction ID
    "Vintage Watch",    // item name
    "Rare 1960s watch", // description
    "seller123",        // seller ID
    100.0,              // base price
    60,                 // duration in minutes
    "collectibles"      // category
);

boolean success = db.saveAuction(auction);
```

#### Save a Bid
```java
Bid bid = new Bid("AUC001", "buyer456", 150.0);
boolean success = db.saveBid(bid);
```

### Loading Data

#### Load a Single Auction
```java
Auction auction = db.loadAuction("AUC001");
```

#### Load All Auctions
```java
Map<String, Auction> auctions = db.loadAllAuctions();
```

#### Load Auctions by Status
```java
List<Auction> activeAuctions = db.loadAuctionsByStatus(AuctionStatus.ACTIVE);
List<Auction> closedAuctions = db.loadAuctionsByStatus(AuctionStatus.CLOSED);
```

#### Load Auctions by Seller
```java
List<Auction> sellerAuctions = db.loadAuctionsBySeller("seller123");
```

#### Load Bids for an Auction
```java
List<Bid> bids = db.loadBidsByAuction("AUC001");
```

#### Load Bids by User
```java
List<Bid> userBids = db.loadBidsByUser("buyer456");
```

#### Get Recent Bid History
```java
// Get last 10 bids for an auction
List<Bid> recentBids = db.getBidHistory("AUC001", 10);
```

### Updating Data

#### Update Auction Status
```java
db.updateAuctionStatus("AUC001", AuctionStatus.CLOSED);
```

### Deleting Data

#### Delete an Auction (and all its bids)
```java
boolean success = db.deleteAuction("AUC001");
```

### Statistics

#### Get Auction Counts
```java
int activeCount = db.getAuctionCount(AuctionStatus.ACTIVE);
int closedCount = db.getAuctionCount(AuctionStatus.CLOSED);
```

#### Get Bid Count for Auction
```java
int bidCount = db.getBidCount("AUC001");
```

### Database Backup

```java
String backupPath = "data/backups/auction_backup_" + 
                    System.currentTimeMillis() + ".db";
boolean success = db.backupDatabase(backupPath);
```

## Integration with Existing Code

### In AuctionManager

Replace file storage with database storage:

```java
// Instead of:
// AuctionFileStorage.getInstance().saveAuction(auction);

// Use:
DatabaseManager.getInstance().saveAuction(auction);
```

### In AuctionServer Startup

Load existing auctions from database:

```java
Map<String, Auction> savedAuctions = DatabaseManager.getInstance().loadAllAuctions();
auctionManager.loadAuctions(savedAuctions);
```

### When Processing Bids

Save each bid to the database:

```java
public boolean placeBid(Bid bid) {
    Auction auction = getAuction(bid.getAuctionId());
    if (auction != null && auction.placeBid(bid)) {
        // Save bid to database
        DatabaseManager.getInstance().saveBid(bid);
        
        // Update auction in database
        DatabaseManager.getInstance().saveAuction(auction);
        
        return true;
    }
    return false;
}
```

## Database File Location

The SQLite database is stored at:
```
data/auction_system.db
```

You can view and query this database using:
- SQLite command-line tool: `sqlite3 data/auction_system.db`
- DB Browser for SQLite: https://sqlitebrowser.org/
- Any SQLite GUI client

## Example Queries

### View All Auctions
```sql
SELECT * FROM auctions;
```

### View Active Auctions
```sql
SELECT * FROM auctions WHERE status = 'ACTIVE';
```

### View Bid History for an Auction
```sql
SELECT * FROM bids 
WHERE auction_id = 'AUC001' 
ORDER BY timestamp DESC;
```

### View Top Bidders
```sql
SELECT user_id, COUNT(*) as bid_count, MAX(amount) as max_bid
FROM bids
GROUP BY user_id
ORDER BY bid_count DESC;
```

### View Auction Statistics
```sql
SELECT 
    status,
    COUNT(*) as count,
    AVG(current_highest_bid) as avg_price,
    MAX(current_highest_bid) as max_price
FROM auctions
GROUP BY status;
```

## Benefits of SQLite Integration

1. **Persistence**: Data survives server restarts
2. **ACID Compliance**: Atomic, consistent, isolated, durable transactions
3. **Scalability**: Handles thousands of auctions and bids efficiently
4. **Query Power**: Rich SQL queries for analytics and reporting
5. **No External Dependencies**: Embedded database, no separate server needed
6. **Concurrent Access**: Thread-safe operations with proper locking
7. **Easy Backup**: Simple file-based backup and restore
8. **Cross-Platform**: Works on Windows, macOS, and Linux

## Troubleshooting

### "SQLite JDBC driver not found"
- Run `./download-dependencies.sh` to download the driver
- Ensure `lib/sqlite-jdbc-*.jar` exists

### Database locked errors
- Ensure only one server instance is running
- Check that `DatabaseManager.close()` is called on shutdown

### Missing data after restart
- Verify database file exists at `data/auction_system.db`
- Check server logs for database errors
- Ensure `saveAuction()` and `saveBid()` are called after changes

## Migration from File Storage

If you have existing auction data in the file-based storage:

1. Keep the old `AuctionFileStorage` temporarily
2. Load auctions using `AuctionFileStorage.getInstance().loadAllAuctions()`
3. Save them to the database using `DatabaseManager.getInstance().saveAuction(auction)`
4. Verify data migration
5. Switch to database-only storage

## Performance Tips

1. **Batch Operations**: Use transactions for multiple inserts
2. **Indexing**: The provided indexes optimize common queries
3. **Connection Pooling**: The singleton pattern reuses connections
4. **Regular Backups**: Schedule periodic database backups
5. **Cleanup**: Periodically delete old closed auctions to maintain performance

## Security Considerations

1. **File Permissions**: Ensure `data/` directory has appropriate permissions
2. **SQL Injection**: PreparedStatements prevent SQL injection attacks
3. **Data Validation**: Validate input before database operations
4. **Backup Security**: Protect backup files with proper permissions

## Future Enhancements

Potential improvements for the database layer:

1. Connection pooling for better concurrency
2. Async database operations
3. Database triggers for automatic status updates
4. Full-text search for auction items
5. Materialized views for analytics
6. Database migration framework
7. Replication for high availability
