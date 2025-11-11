# Module 2: Auction Creation System

## Overview

This module implements the **Auction Creation** functionality for the Real-Time Auction Platform. It enables sellers to create auctions, broadcasts them to all connected clients, and persists auction details using Java I/O.

## Developer

**Module 2 - Auction Creation**

## Key Features Implemented

### 1. Enhanced Auction Model (`Auction.java`)

- ✅ Complete auction properties (item name, description, seller ID, base price, category)
- ✅ Time-based auction management (creation time, end time, duration)
- ✅ Auction status tracking (ACTIVE, CLOSED, CANCELLED)
- ✅ Automatic expiration checking
- ✅ Thread-safe bid placement
- ✅ Serializable for file storage
- ✅ Broadcast formatting for real-time updates

### 2. File-Based Persistence (`AuctionFileStorage.java`)

- ✅ Save/load individual auctions
- ✅ Bulk auction operations
- ✅ Index-based tracking of all auctions
- ✅ Backup and restore functionality
- ✅ Text export for debugging
- ✅ Storage statistics and monitoring

### 3. Auction Management (`AuctionManager.java`)

- ✅ Create auctions with full parameters
- ✅ Automatic ID generation
- ✅ Load auctions from storage on startup
- ✅ Filter by category, seller, or status
- ✅ Close and cancel auctions
- ✅ Integration with broadcast system
- ✅ Statistics and monitoring

### 4. Server Integration (`AuctionServer.java`)

- ✅ CREATE_AUCTION command handler
- ✅ LIST_AUCTIONS command handler
- ✅ GET_AUCTION command handler
- ✅ Real-time broadcast to all clients
- ✅ Proper error handling and validation

### 5. Test Client (`AuctionCreatorClient.java`)

- ✅ Interactive menu-driven interface
- ✅ Create auctions with all parameters
- ✅ List and filter auctions
- ✅ View auction details
- ✅ Watch and bid on auctions
- ✅ Real-time updates display

## File Structure

```
src/main/
├── model/
│   └── Auction.java              [ENHANCED] - Complete auction model
├── server/
│   ├── AuctionManager.java       [ENHANCED] - Auction creation & management
│   └── AuctionServer.java        [ENHANCED] - Command handlers & broadcasting
├── util/
│   └── AuctionFileStorage.java   [NEW] - File I/O persistence layer
└── client/
    └── AuctionCreatorClient.java [NEW] - Test client for auction creation

data/
├── auctions/                     [AUTO-CREATED] - Auction storage
│   ├── auction-1.dat
│   ├── auction-1.txt
│   ├── index.dat
│   └── ...
└── backups/                      [AUTO-CREATED] - Backup storage
    └── auctions_backup_*.dat
```

## Protocol Specification

### Commands (Client → Server)

#### 1. CREATE_AUCTION

**Format:**

```
CREATE_AUCTION:itemName:description:sellerId:basePrice:durationMinutes:category
```

**Example:**

```
CREATE_AUCTION:Vintage Watch:Rare 1960s timepiece:seller123:500.00:60:collectibles
```

**Response:**

```
AUCTION_CREATED:auction-1:Vintage Watch
AUCTION_DETAILS:auction-1:Vintage Watch:Rare 1960s timepiece:500.00:500.00:none:3600:collectibles:ACTIVE
```

#### 2. LIST_AUCTIONS

**Format:**

```
LIST_AUCTIONS              # List all active auctions
LIST_AUCTIONS:category     # List auctions by category
```

**Response:**

```
AUCTIONS_LIST:all
NEW_AUCTION:auction-1:Vintage Watch:...:3600:collectibles:seller123
NEW_AUCTION:auction-2:Painting:...:1800:art:seller456
AUCTIONS_LIST_END
```

#### 3. GET_AUCTION

**Format:**

```
GET_AUCTION:auctionId
```

**Response:**

```
AUCTION_DETAILS:auction-1:Vintage Watch:Rare 1960s timepiece:500.00:750.00:bidder1:2400:collectibles:ACTIVE
```

### Broadcasts (Server → All Clients)

#### NEW_AUCTION

Automatically sent to all connected clients when an auction is created:

```
NEW_AUCTION:auctionId:itemName:description:basePrice:timeRemaining:category:sellerId
```

## Usage Examples

### Creating an Auction (Programmatically)

```java
// In your server code or another module
AuctionManager manager = new AuctionManager();

Auction auction = manager.createAuction(
    "Gaming Laptop",                    // itemName
    "High-end gaming laptop, RTX 4090", // description
    "seller123",                        // sellerId
    1500.00,                           // basePrice
    120,                               // duration (minutes)
    "electronics"                      // category
);

// Auction is automatically saved to file and broadcast to all clients
```

### Creating an Auction (Via Client)

```bash
# Start the test client
java -cp bin main.client.AuctionCreatorClient localhost 8080

# Use the interactive menu
> create

Item Name: Vintage Camera
Description: Canon AE-1 from 1976
Seller ID: photoenthusiast
Base Price: $250.00
Duration (minutes): 90
Category: photography
```

### Listing Auctions

```bash
# List all active auctions
> list

# List auctions by category
> listcat
Enter category: electronics
```

### Viewing Auction Details

```bash
> details
Enter auction ID: auction-1
```

## Persistence Details

### Storage Format

- **Binary Files**: Auctions are serialized using Java ObjectOutputStream
- **Index File**: Tracks all auction IDs for fast loading
- **Text Export**: Human-readable format for debugging

### Storage Location

```
data/auctions/
├── auction-1.dat      # Binary auction data
├── auction-1.txt      # Text export (optional)
├── auction-2.dat
├── auction-2.txt
└── index.dat          # Index of all auction IDs
```

### Automatic Loading

- Auctions are automatically loaded from storage when server starts
- Expired auctions are marked as CLOSED
- Auction ID counter is updated to prevent conflicts

### Backup System

```java
// Create a backup (can be scheduled)
auctionManager.createBackup();

// Backup saved to: data/backups/auctions_backup_<timestamp>.dat
```

## Integration with Other Modules

### Module 1 (User Authentication)

```java
// TODO: Replace "tempUser" with actual authenticated user
String userId = authManager.getAuthenticatedUser(sender);
```

### Module 3 (Bidding System)

```java
// Bidding system uses the enhanced placeBid method
boolean success = auction.placeBid(bid);
// Status checking and expiration are handled automatically
```

### Module 4 (Real-Time Broadcasting)

```java
// Auction creation automatically triggers broadcast
server.broadcastNewAuction(auction);

// BidBroadcaster uses auction.getWatchers() for targeted updates
```

### Module 5 (Chat System)

```java
// Can integrate auction details into chat
String auctionInfo = auction.toDetailString();
chatHandler.sendMessage(auctionInfo);
```

## Testing

### Running the Test Client

```bash
# Compile
javac -d bin src/main/client/AuctionCreatorClient.java src/main/model/*.java

# Run
java -cp bin main.client.AuctionCreatorClient localhost 8080
```

### Test Scenarios

1. **Create Multiple Auctions**

   - Create 3+ auctions with different categories
   - Verify each gets unique ID
   - Check file storage (data/auctions/)

2. **Test Persistence**

   - Create auctions
   - Stop server
   - Restart server
   - Verify auctions are loaded

3. **Test Expiration**

   - Create auction with 1-minute duration
   - Wait 1 minute
   - Try to bid (should fail - auction closed)

4. **Test Broadcasting**

   - Connect 2+ clients
   - Create auction from one client
   - Verify all clients receive broadcast

5. **Test Categories**
   - Create auctions in different categories
   - List by category
   - Verify filtering works

## Error Handling

The module handles various error cases:

- ❌ Invalid command format → "ERROR: Invalid CREATE_AUCTION format..."
- ❌ Empty required fields → "ERROR: Item name and seller ID cannot be empty"
- ❌ Invalid price → "ERROR: Base price must be greater than 0"
- ❌ Invalid duration → "ERROR: Duration must be greater than 0"
- ❌ Auction not found → "ERROR: Auction not found: auction-X"
- ❌ File I/O errors → Logged to console with details

## Statistics and Monitoring

```java
// Print auction statistics
auctionManager.printStatistics();

// Print storage statistics
AuctionFileStorage.getInstance().printStorageStats();
```

**Output:**

```
=== AUCTION MANAGER STATISTICS ===
Total Auctions: 15
Active: 8
Closed: 5
Cancelled: 2
===================================

=== AUCTION STORAGE STATISTICS ===
Total Auctions: 15
Active: 8
Closed: 5
Cancelled: 2
Storage Location: C:\...\Backend\data\auctions
==================================
```

## Thread Safety

All operations are thread-safe:

- ✅ `ConcurrentHashMap` for auction storage
- ✅ `AtomicInteger` for ID generation
- ✅ `synchronized` methods in file storage
- ✅ `CopyOnWriteArrayList` for watchers

## Configuration

Add to `config.properties`:

```properties
# Auction settings
auction.default.duration=60
auction.min.price=1.00
auction.storage.dir=data/auctions/
auction.backup.enabled=true
auction.backup.interval=3600000
```

## Future Enhancements

Possible improvements:

- [ ] Image upload support
- [ ] Scheduled auction start times
- [ ] Reserve price (minimum acceptable bid)
- [ ] Buy-it-now option
- [ ] Auction templates
- [ ] Database storage (SQLite)
- [ ] Search functionality
- [ ] Auction history/analytics

## Conclusion

This module provides a complete, production-ready auction creation system with:

- ✅ Full CRUD operations
- ✅ Persistent storage
- ✅ Real-time broadcasting
- ✅ Thread-safe operations
- ✅ Comprehensive error handling
- ✅ Easy integration with other modules

The implementation is clean, well-documented, and doesn't interfere with your teammates' work while providing all necessary hooks for integration.
