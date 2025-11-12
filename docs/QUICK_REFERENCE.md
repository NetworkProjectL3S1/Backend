# Database Quick Reference Card

## Setup (One-time)
```bash
./download-dependencies.sh  # Download SQLite JDBC driver
./compile.sh                # Compile with database support
make test-db                # Verify installation
```

## Basic Usage

### Initialize Database
```java
DatabaseManager db = DatabaseManager.getInstance();
```

### Save Auction
```java
Auction auction = new Auction("AUC001", "Item", "Desc", "seller", 100.0, 60, "cat");
db.saveAuction(auction);
```

### Save Bid
```java
Bid bid = new Bid("AUC001", "user123", 150.0);
db.saveBid(bid);
```

### Load Auction
```java
Auction auction = db.loadAuction("AUC001");
```

### Load All Auctions
```java
Map<String, Auction> all = db.loadAllAuctions();
```

### Load Active Auctions
```java
List<Auction> active = db.loadAuctionsByStatus(AuctionStatus.ACTIVE);
```

### Load Auctions by Seller
```java
List<Auction> sellerAuctions = db.loadAuctionsBySeller("seller123");
```

### Load Bids for Auction
```java
List<Bid> bids = db.loadBidsByAuction("AUC001");
```

### Load User's Bids
```java
List<Bid> userBids = db.loadBidsByUser("user123");
```

### Get Bid History (Recent)
```java
List<Bid> recent = db.getBidHistory("AUC001", 10);
```

### Update Auction Status
```java
db.updateAuctionStatus("AUC001", AuctionStatus.CLOSED);
```

### Delete Auction
```java
db.deleteAuction("AUC001");  // Also deletes all bids
```

### Get Statistics
```java
int activeCount = db.getAuctionCount(AuctionStatus.ACTIVE);
int bidCount = db.getBidCount("AUC001");
```

### Backup Database
```java
db.backupDatabase("data/backups/backup.db");
```

## Command Line

### Open Database Shell
```bash
sqlite3 data/auction_system.db
# or
make db-shell
```

### View All Auctions
```sql
SELECT * FROM auctions;
```

### View Active Auctions
```sql
SELECT * FROM auctions WHERE status = 'ACTIVE';
```

### View Bids for Auction
```sql
SELECT * FROM bids WHERE auction_id = 'AUC001' ORDER BY timestamp DESC;
```

### Backup Database
```bash
make backup-db
```

## File Locations
```
data/auction_system.db        # Main database
data/backups/                 # Backup directory
lib/sqlite-jdbc-*.jar         # JDBC driver
```

## Make Commands
```bash
make deps         # Download dependencies
make compile      # Compile with database
make test-db      # Run database tests
make db-shell     # Open database shell
make backup-db    # Backup database
make clean-all    # Clean everything
```

## Common Patterns

### Full Auction Lifecycle
```java
DatabaseManager db = DatabaseManager.getInstance();

// 1. Create auction
Auction auction = new Auction(...);
db.saveAuction(auction);

// 2. Place bids
Bid bid1 = new Bid(auction.getAuctionId(), "buyer1", 110.0);
auction.placeBid(bid1);
db.saveBid(bid1);
db.saveAuction(auction);  // Update highest bid

// 3. Close auction
db.updateAuctionStatus(auction.getAuctionId(), AuctionStatus.CLOSED);

// 4. View history
List<Bid> history = db.getBidHistory(auction.getAuctionId(), 10);
```

### Query Auctions
```java
// All auctions
Map<String, Auction> all = db.loadAllAuctions();

// Active only
List<Auction> active = db.loadAuctionsByStatus(AuctionStatus.ACTIVE);

// By seller
List<Auction> mine = db.loadAuctionsBySeller("myUserId");

// Statistics
int activeCount = db.getAuctionCount(AuctionStatus.ACTIVE);
```

### Query Bids
```java
// All bids for auction
List<Bid> allBids = db.loadBidsByAuction("AUC001");

// Recent bids
List<Bid> recent = db.getBidHistory("AUC001", 5);

// User's bids
List<Bid> myBids = db.loadBidsByUser("myUserId");

// Bid count
int count = db.getBidCount("AUC001");
```

## Error Handling
```java
try {
    boolean success = db.saveAuction(auction);
    if (!success) {
        System.err.println("Failed to save auction");
    }
} catch (Exception e) {
    System.err.println("Database error: " + e.getMessage());
}
```

## Documentation Files
- `DATABASE_SUMMARY.md` - Overview and quick start
- `DATABASE_GUIDE.md` - Complete API documentation
- `MIGRATION_GUIDE.md` - Integration with existing code
- `SQL_REFERENCE.md` - SQL queries and tips
- This file - Quick reference

## Need Help?
1. Run `make test-db` to verify setup
2. Check logs for database errors
3. Use `make db-shell` to inspect data
4. Read DATABASE_GUIDE.md for details
