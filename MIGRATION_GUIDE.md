# Migration Guide: Integrating SQLite Database

This guide shows you how to integrate the new SQLite database with your existing auction system code.

## Step 1: Understand What Changes

### Current Architecture (File-Based)
```
AuctionManager → AuctionFileStorage → .dat files
```

### New Architecture (Database)
```
AuctionManager → DatabaseManager → SQLite Database
```

## Step 2: Update AuctionManager

Find your `AuctionManager` class and update the storage methods.

### Add Database Import
```java
import main.util.DatabaseManager;
```

### Replace File Storage in Constructor/Init
```java
// Old code:
// private AuctionFileStorage storage = AuctionFileStorage.getInstance();

// New code:
private DatabaseManager db = DatabaseManager.getInstance();
```

### Update Load Auctions Method
```java
public void loadAuctions() {
    // Old code:
    // Map<String, Auction> savedAuctions = storage.loadAllAuctions();
    
    // New code:
    Map<String, Auction> savedAuctions = db.loadAllAuctions();
    
    for (Auction auction : savedAuctions.values()) {
        auctions.put(auction.getAuctionId(), auction);
    }
    
    System.out.println("[AuctionManager] Loaded " + savedAuctions.size() + " auctions from database");
}
```

### Update Save Auction Method
```java
public boolean createAuction(Auction auction) {
    if (auction == null) {
        return false;
    }
    
    // Add to memory
    auctions.put(auction.getAuctionId(), auction);
    
    // Old code:
    // return storage.saveAuction(auction);
    
    // New code:
    return db.saveAuction(auction);
}
```

## Step 3: Update Bid Processing

In your bid handling code (likely in `AuctionClientHandler` or `BidBroadcaster`):

### Save Bids to Database
```java
public boolean processBid(Bid bid) {
    Auction auction = auctionManager.getAuction(bid.getAuctionId());
    
    if (auction == null) {
        return false;
    }
    
    // Attempt to place bid
    if (auction.placeBid(bid)) {
        // Save bid to database (NEW!)
        db.saveBid(bid);
        
        // Update auction in database
        db.saveAuction(auction);
        
        // Broadcast to watchers
        broadcastBid(bid);
        
        return true;
    }
    
    return false;
}
```

## Step 4: Update AuctionServer Initialization

In `AuctionServer` or `ServerMain`:

### Initialize Database and Load Auctions
```java
public void startServer(int port) throws IOException {
    // Initialize database (creates tables if needed)
    DatabaseManager db = DatabaseManager.getInstance();
    
    // Load existing auctions from database
    auctionManager.loadAuctions();
    
    // Start server socket
    serverSocket = new ServerSocket(port);
    System.out.println("[AuctionServer] Server started on port " + port);
    
    // Rest of server initialization...
}
```

## Step 5: Handle Auction Expiration

Update your auction expiration/cleanup code:

### Close Expired Auctions
```java
private void checkExpiredAuctions() {
    for (Auction auction : auctionManager.getAllAuctions()) {
        if (auction.getStatus() == AuctionStatus.ACTIVE && auction.hasExpired()) {
            // Update status
            auction.setStatus(AuctionStatus.CLOSED);
            
            // Save to database (NEW!)
            db.updateAuctionStatus(auction.getAuctionId(), AuctionStatus.CLOSED);
            
            // Notify watchers
            notifyAuctionClosed(auction);
        }
    }
}
```

## Step 6: Add Shutdown Hook (Optional but Recommended)

In your main server class:

```java
public static void main(String[] args) {
    // ... server setup code ...
    
    // Add shutdown hook to ensure clean database closure
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("[Server] Shutting down...");
        
        // Save all active auctions
        for (Auction auction : auctionManager.getAllAuctions()) {
            db.saveAuction(auction);
        }
        
        // Close database connection
        db.close();
        
        System.out.println("[Server] Shutdown complete");
    }));
    
    // Start server
    server.start();
}
```

## Step 7: Add User Features (Optional)

### Get User's Bidding History
```java
public List<Bid> getUserBids(String userId) {
    return db.loadBidsByUser(userId);
}
```

### Get User's Auctions
```java
public List<Auction> getUserAuctions(String sellerId) {
    return db.loadAuctionsBySeller(sellerId);
}
```

### Get Auction Statistics
```java
public void showStatistics() {
    int active = db.getAuctionCount(AuctionStatus.ACTIVE);
    int closed = db.getAuctionCount(AuctionStatus.CLOSED);
    
    System.out.println("Active Auctions: " + active);
    System.out.println("Closed Auctions: " + closed);
}
```

## Step 8: Testing Migration

### 1. Backup Existing Data
```bash
# If you have existing .dat files
mkdir -p data/old_file_storage
cp -r data/auctions data/old_file_storage/
```

### 2. Test Database Operations
```bash
make test-db
```

### 3. Run Server and Create Test Auction
```bash
./start-server.sh
```

Then create an auction and verify it's saved to database:
```bash
sqlite3 data/auction_system.db "SELECT * FROM auctions;"
```

### 4. Restart Server and Verify Persistence
Stop the server and restart it. Your auctions should be loaded from the database.

## Complete Example: Updated AuctionManager

Here's a complete example of how your `AuctionManager` might look:

```java
package main.server;

import main.model.Auction;
import main.model.Auction.AuctionStatus;
import main.model.Bid;
import main.util.DatabaseManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private Map<String, Auction> auctions;
    private DatabaseManager db;
    
    public AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
        this.db = DatabaseManager.getInstance();
        
        // Load existing auctions from database
        loadAuctions();
    }
    
    /**
     * Load all auctions from database
     */
    public void loadAuctions() {
        Map<String, Auction> savedAuctions = db.loadAllAuctions();
        auctions.putAll(savedAuctions);
        System.out.println("[AuctionManager] Loaded " + savedAuctions.size() + " auctions");
    }
    
    /**
     * Create and save a new auction
     */
    public boolean createAuction(Auction auction) {
        if (auction == null || auctions.containsKey(auction.getAuctionId())) {
            return false;
        }
        
        // Add to memory
        auctions.put(auction.getAuctionId(), auction);
        
        // Save to database
        return db.saveAuction(auction);
    }
    
    /**
     * Process a bid on an auction
     */
    public boolean processBid(Bid bid) {
        Auction auction = auctions.get(bid.getAuctionId());
        
        if (auction == null) {
            return false;
        }
        
        // Attempt to place bid
        synchronized (auction) {
            if (auction.placeBid(bid)) {
                // Save bid to database
                db.saveBid(bid);
                
                // Update auction in database
                db.saveAuction(auction);
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get auction by ID
     */
    public Auction getAuction(String auctionId) {
        return auctions.get(auctionId);
    }
    
    /**
     * Get all active auctions
     */
    public List<Auction> getActiveAuctions() {
        return db.loadAuctionsByStatus(AuctionStatus.ACTIVE);
    }
    
    /**
     * Close an auction
     */
    public boolean closeAuction(String auctionId) {
        Auction auction = auctions.get(auctionId);
        
        if (auction != null) {
            auction.setStatus(AuctionStatus.CLOSED);
            
            // Update in database
            return db.updateAuctionStatus(auctionId, AuctionStatus.CLOSED);
        }
        
        return false;
    }
    
    /**
     * Get bid history for an auction
     */
    public List<Bid> getBidHistory(String auctionId, int limit) {
        return db.getBidHistory(auctionId, limit);
    }
    
    /**
     * Get user's auctions
     */
    public List<Auction> getUserAuctions(String userId) {
        return db.loadAuctionsBySeller(userId);
    }
    
    /**
     * Get user's bids
     */
    public List<Bid> getUserBids(String userId) {
        return db.loadBidsByUser(userId);
    }
    
    /**
     * Get all auctions (from memory)
     */
    public Map<String, Auction> getAllAuctions() {
        return new ConcurrentHashMap<>(auctions);
    }
}
```

## Migration Checklist

- [ ] Download SQLite JDBC driver (`./download-dependencies.sh`)
- [ ] Compile with updated classpath (`./compile.sh`)
- [ ] Run database tests (`make test-db`)
- [ ] Update imports in AuctionManager
- [ ] Replace AuctionFileStorage with DatabaseManager
- [ ] Update createAuction() to use database
- [ ] Update processBid() to save bids
- [ ] Update loadAuctions() to load from database
- [ ] Add auction status updates to database
- [ ] Add shutdown hook for clean closure
- [ ] Test auction creation and persistence
- [ ] Test bid placement and retrieval
- [ ] Test server restart with data persistence
- [ ] Verify all queries work correctly

## Rollback Plan

If you need to rollback to file storage:

1. Keep both `AuctionFileStorage` and `DatabaseManager` in your code
2. Use a configuration flag to switch between them
3. Export database to files if needed:
   ```java
   Map<String, Auction> dbAuctions = db.loadAllAuctions();
   for (Auction auction : dbAuctions.values()) {
       AuctionFileStorage.getInstance().saveAuction(auction);
   }
   ```

## Benefits After Migration

✅ **Data Persistence**: Auctions survive crashes and restarts
✅ **Query Power**: Rich SQL queries for analytics
✅ **Scalability**: Handle thousands of auctions efficiently
✅ **Integrity**: ACID transactions ensure consistency
✅ **Features**: Easy to add search, filters, statistics
✅ **Maintenance**: Simple backup and restore

## Need Help?

- See `DATABASE_GUIDE.md` for API documentation
- See `SQL_REFERENCE.md` for query examples
- Run `make test-db` to verify setup
- Check `data/auction_system.db` with `sqlite3`

Happy migrating! 🚀
