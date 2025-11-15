# Auction Timer & Expiration Notification System

## Overview
Implemented a comprehensive real-time auction timer and expiration notification system using Java NIO, WebSockets, and React.

## Features Implemented

### Backend Features

#### 1. **AuctionTimerManager** (`Backend-1/src/main/util/AuctionTimerManager.java`)
- **Singleton pattern** for managing all auction timers
- **ScheduledExecutorService** with thread pool for timer scheduling
- **Automatic timer initialization** on server startup for all active auctions
- **Expiration handling** with automatic status updates
- **Real-time WebSocket notifications** to winners and sellers

**Key Methods:**
- `scheduleAuctionExpiration(Auction auction)` - Schedule timer for auction
- `handleAuctionExpiration(Auction auction)` - Process expired auctions
- `broadcastExpiration()` - Send notifications via WebSocket
- `initializeActiveAuctionTimers()` - Load timers on startup
- `getRemainingTime(String auctionId)` - Get time left for auction

#### 2. **WebSocketBidController Enhancement**
- Added `broadcastAuctionExpiration()` method
- Sends structured JSON notifications:
  ```json
  {
    "type": "AUCTION_EXPIRED",
    "auctionId": "...",
    "itemName": "...",
    "winner": "username",
    "seller": "username",
    "finalPrice": 100.00,
    "status": "CLOSED"
  }
  ```

#### 3. **AuctionController Enhancement**
- Automatically schedules timer when auction is created
- Integration with AuctionTimerManager

#### 4. **ApiServer Enhancement**
- Initializes timers on startup: `AuctionTimerManager.getInstance().initializeActiveAuctionTimers()`
- Graceful shutdown of timer manager on server stop

#### 5. **BidController Enhancement**
- Already prevents bids on expired auctions
- Thread-safe with ReentrantLock
- Checks `auction.hasExpired()` before processing bids

### Frontend Features

#### 1. **AuctionPage Component** (`Frontend-1/auction-client/src/pages/AuctionPage.jsx`)

**New State Variables:**
- `timeRemaining` - Milliseconds until auction ends
- `isExpired` - Boolean flag for expired status
- `showWinnerNotification` - Winner notification display
- `showSellerNotification` - Seller notification display

**Timer Countdown:**
- Updates every second
- Shows human-readable format (e.g., "2h 15m 30s", "45m 30s", "15s")
- Automatically sets `isExpired` when time reaches zero

**WebSocket Integration:**
- Listens for `AUCTION_EXPIRED` events
- Updates auction status to CLOSED
- Shows personalized notifications for winners and sellers
- Maintains existing `BID_UPDATE` functionality

**UI Enhancements:**
- **Live countdown timer** replacing static "Ends At" timestamp
- **Expired indicator** with red styling when auction ends
- **Winner notification card** (green) with trophy icon
- **Seller notification card** (blue) with sale details
- **Disabled bid panel** when auction expires
- **Visual feedback** for expired state (red borders, badges)

#### 2. **BidPanel Component Enhancement**

**New Props:**
- `disabled` - Boolean to disable bidding

**Features:**
- Visual styling changes when disabled (red borders)
- "Auction Expired" title when disabled
- "Bidding Closed" button text when disabled
- Prevents bid placement on expired auctions
- Shows error message if user attempts to bid on expired auction

## How It Works

### Auction Creation Flow
1. User creates auction via API
2. `AuctionController` saves auction to database
3. `AuctionController` calls `AuctionTimerManager.scheduleAuctionExpiration()`
4. Timer scheduled based on `auction.getEndTime()`

### Server Startup Flow
1. `ApiServer.start()` calls `AuctionTimerManager.initializeActiveAuctionTimers()`
2. Manager fetches all ACTIVE auctions from database
3. For each auction:
   - If already expired → immediately process expiration
   - If still active → schedule timer

### Expiration Flow
1. Timer fires when `endTime` reached
2. `AuctionTimerManager.handleAuctionExpiration()` called
3. Auction status updated to CLOSED in database
4. Winner and seller information extracted
5. `WebSocketBidController.broadcastAuctionExpiration()` sends notifications
6. All subscribed clients receive expiration event

### Frontend Flow
1. **Timer Update (every 1 second):**
   - Calculate `remaining = endTime - Date.now()`
   - Update `timeRemaining` state
   - Set `isExpired = true` when remaining ≤ 0

2. **WebSocket Event:**
   - Receive `AUCTION_EXPIRED` event
   - Update auction status to CLOSED
   - Check if user is winner or seller
   - Show appropriate notification

3. **UI Updates:**
   - Timer shows "EXPIRED" in red
   - Status badge changes to "EXPIRED"
   - Bid panel becomes disabled
   - Winner/seller sees personalized notification

## Testing

### Create Test Auction (30 seconds)
```bash
cd Backend-1
./test-expiration.sh
```

This creates an auction that expires in 30 seconds for testing purposes.

### Manual Testing Steps
1. **Start Backend:**
   ```bash
   cd Backend-1
   ./start-api-server.sh
   ```

2. **Start Frontend:**
   ```bash
   cd Frontend-1/auction-client
   npm run dev
   ```

3. **Test Scenario:**
   - Login as seller
   - Create auction with 1-minute duration
   - Login as buyer in another browser/tab
   - Place some bids
   - Watch the countdown timer
   - Observe expiration notification when timer hits zero
   - Verify bidding is disabled after expiration

### Expected Behavior

**Before Expiration:**
- ✅ Live countdown timer shows remaining time
- ✅ Bids can be placed
- ✅ New bids update in real-time
- ✅ Status badge shows "ACTIVE"

**At Expiration:**
- ✅ Timer shows "EXPIRED" in red
- ✅ Status badge changes to "EXPIRED"
- ✅ Winner sees green notification with trophy icon
- ✅ Seller sees blue notification with winner details
- ✅ Bid panel becomes disabled
- ✅ "Bidding Closed" button appears

**After Expiration:**
- ✅ Cannot place new bids
- ✅ Auction remains viewable
- ✅ Final bid history preserved
- ✅ Winner and price clearly displayed

## Technical Details

### Time Format Function
```javascript
const formatTimeRemaining = (ms) => {
  if (ms <= 0) return 'EXPIRED';
  
  const seconds = Math.floor((ms / 1000) % 60);
  const minutes = Math.floor((ms / (1000 * 60)) % 60);
  const hours = Math.floor((ms / (1000 * 60 * 60)) % 24);
  const days = Math.floor(ms / (1000 * 60 * 60 * 24));
  
  if (days > 0) return `${days}d ${hours}h ${minutes}m`;
  if (hours > 0) return `${hours}h ${minutes}m ${seconds}s`;
  if (minutes > 0) return `${minutes}m ${seconds}s`;
  return `${seconds}s`;
};
```

### Timer Scheduling (Java)
```java
long delay = endTime - System.currentTimeMillis();
ScheduledFuture<?> future = scheduler.schedule(
    () -> handleAuctionExpiration(auction),
    delay,
    TimeUnit.MILLISECONDS
);
```

### Notification JSON Structure
```json
{
  "type": "AUCTION_EXPIRED",
  "auctionId": "auction-1234567890-xxx",
  "itemName": "Vintage Camera",
  "winner": "buyer1",
  "seller": "seller1",
  "finalPrice": 250.00,
  "status": "CLOSED"
}
```

## Files Modified

### Backend
- ✅ `src/main/util/AuctionTimerManager.java` (NEW)
- ✅ `src/main/api/controllers/WebSocketBidController.java`
- ✅ `src/main/api/controllers/AuctionController.java`
- ✅ `src/main/api/ApiServer.java`
- ✅ `test-expiration.sh` (NEW)

### Frontend
- ✅ `src/pages/AuctionPage.jsx`
- ✅ `src/components/BidPanel.jsx`

## Database Schema
No changes required - uses existing `Auction` table with:
- `end_time` - Timestamp when auction expires
- `status` - ACTIVE/CLOSED/CANCELLED
- `current_highest_bidder` - Winner username

## Performance Considerations

1. **Timer Efficiency:**
   - Uses `ScheduledExecutorService` with thread pool
   - Minimal overhead per auction
   - Timers cleaned up after execution

2. **WebSocket Broadcasting:**
   - Only sends to subscribed clients for that auction
   - JSON payload under 200 bytes
   - No database queries during broadcast

3. **Frontend Updates:**
   - Timer updates every 1 second (not every millisecond)
   - React state updates optimized with functional setState
   - No unnecessary re-renders

## Future Enhancements

Possible improvements:
1. Email notifications for winners/sellers
2. Auction extension (if bid placed in last minute)
3. Scheduled auctions (start time + end time)
4. Automatic auction cleanup after X days
5. Auction statistics dashboard
6. Push notifications for mobile apps

## Troubleshooting

**Timer not starting:**
- Check server logs for `[AuctionTimerManager]` messages
- Verify auction status is ACTIVE
- Ensure endTime is in future

**Notifications not received:**
- Check browser console for WebSocket messages
- Verify user is subscribed to auction
- Check network tab for WebSocket connection

**Bidding still possible after expiration:**
- Check frontend `isExpired` state
- Verify WebSocket event received
- Check backend `auction.hasExpired()` logic

## Conclusion

The auction timer and expiration system provides:
- ✅ Real-time countdown visualization
- ✅ Automatic auction closure at expiration
- ✅ Personalized notifications for winners and sellers
- ✅ Prevention of late bids
- ✅ Clean user experience with visual feedback
- ✅ Thread-safe timer management
- ✅ Efficient WebSocket-based notifications
