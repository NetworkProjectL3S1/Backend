# Chat System Debugging Guide

## Overview
This guide explains how to debug the real-time chat messaging system, specifically focusing on seller-to-buyer message delivery issues.

## System Architecture

### Backend Components
1. **ChatServer** (Port 8080) - WebSocket server
   - Manages client connections
   - Routes messages between clients
   - Logs all broadcast and direct messages

2. **ClientHandler** - Per-client handler
   - Processes incoming messages
   - Handles private messages
   - Saves messages to database
   - Sends messages via WebSocket

3. **DatabaseManager** - SQLite persistence
   - Stores messages in `chat_messages` table
   - Provides message retrieval by auction

### Frontend Components
1. **auctionChatService.js** - WebSocket client
   - Connects to ChatServer
   - Subscribes to auctions
   - Dispatches messages to handlers

2. **ChatBox.jsx** - Buyer chat UI
   - Displays messages for one auction
   - Subscribes to auction messages
   - Handles message rendering

3. **SellerChatsPage.jsx** - Seller chat dashboard
   - Shows all seller's auction chats
   - Subscribes to multiple auctions
   - Manages message state per auction

## Enhanced Logging System

### Backend Logs (ChatServer Terminal)

#### ClientHandler Logs
```
[ClientHandler] 📨 Received private message from: <sender>
[ClientHandler] Message content: <content>
[ClientHandler] Target username: <recipient>
[ClientHandler] Auction ID: <auctionId>
[ClientHandler] ✅ Found target user: <recipient>
[ClientHandler] 💾 Saving message to database...
[ClientHandler] ✅ Message saved to database successfully
[ClientHandler] 📨 Sending to <recipient>: [Private from <sender>] [Auction:<id>] <content>
[ClientHandler] ✅ Message sent successfully to <recipient>
[ClientHandler] 📨 Sending to <sender>: [Private to <recipient>] [Auction:<id>] <content>
[ClientHandler] ✅ Message sent successfully to <sender>
```

#### ChatServer Logs
```
[ChatServer] 📨 Attempting to send message to user: <username>
[ChatServer] Message content: <content>
[ChatServer] ✅ Sending formatted message: <formatted>
[ChatServer] ✅ Message sent successfully to <username>
```

### Frontend Logs (Browser Console)

#### auctionChatService Logs
```
[AuctionChatService] 📨 Raw WebSocket message: <message>
[AuctionChatService] 📋 Parsed message - Sender: <sender>, Auction: <id>, Content: <content>
[AuctionChatService] 🔍 Available auction handlers: [<ids>]
[AuctionChatService] ✅ Dispatching to auction <id> handler
```

#### ChatBox Logs (Buyer)
```
[ChatBox] 📨 Received WebSocket message from <sender>
[ChatBox] Message details - Sender: <sender>, Content: <content>
[ChatBox] Creating message ID: <id>
[ChatBox] ✅ Message ID <id> is new, adding to state
[ChatBox] Message array updated, new length: <count>
```

#### SellerChatsPage Logs
```
[SellerChats] 📨 Received WebSocket message for auction <id>
[SellerChats] Raw message: <message>
[SellerChats] 👤 Extracted sender: <sender>
[SellerChats] Creating message ID: <id>
[SellerChats] ✅ Adding new message from <sender>
```

## Testing Procedure

### Setup
1. **Start Backend Servers**
   ```bash
   # Terminal 1: Chat Server (already running)
   cd Backend-1
   java -cp "build:lib/*" main.server.ChatServer
   
   # Terminal 2: API Server
   cd Backend-1
   java -cp "build:lib/*" main.api.ApiServer
   ```

2. **Start Frontend**
   ```bash
   cd Frontend-1/auction-client
   npm run dev
   ```

### Test Case 1: Seller to Buyer Message (Real-time Issue)

#### Steps:
1. **Open Browser 1 (Buyer)**
   - Open Chrome/Firefox Developer Console (F12)
   - Login as buyer (e.g., `vishwajaya`)
   - Navigate to an auction
   - Open ChatBox for that auction
   - Keep console open

2. **Open Browser 2 (Seller)**
   - Open Chrome/Firefox Developer Console (F12)
   - Login as seller (owner of the auction)
   - Navigate to "Seller Chats" page
   - Find the same auction
   - Keep console open

3. **Send Message from Seller**
   - In Browser 2 (Seller), type a message
   - Click Send
   - **Watch Both Consoles and Backend Terminal**

#### Expected Logs Flow:

**Backend Terminal (ChatServer):**
```
[ClientHandler] 📨 Received private message from: seller1
[ClientHandler] Message content: Hello from seller
[ClientHandler] Target username: vishwajaya
[ClientHandler] Auction ID: 123
[ClientHandler] ✅ Found target user: vishwajaya
[ClientHandler] 💾 Saving message to database...
[ClientHandler] ✅ Message saved to database successfully
[ClientHandler] 📨 Sending to vishwajaya: [Private from seller1] [Auction:123] Hello from seller
[ClientHandler] ✅ Message sent successfully to vishwajaya
[ClientHandler] 📨 Sending to seller1: [Private to vishwajaya] [Auction:123] Hello from seller
[ClientHandler] ✅ Message sent successfully to seller1
```

**Browser 2 (Seller) Console:**
```
[AuctionChatService] 📨 Raw WebSocket message: [Private to vishwajaya] [Auction:123] Hello from seller
[AuctionChatService] 📋 Parsed message - Sender: seller1, Auction: 123, Content: Hello from seller
[AuctionChatService] 🔍 Available auction handlers: [123]
[AuctionChatService] ✅ Dispatching to auction 123 handler
[SellerChats] 📨 Received WebSocket message for auction 123
[SellerChats] 👤 Extracted sender: seller1
[SellerChats] ✅ Adding new message from seller1
```

**Browser 1 (Buyer) Console - THIS IS WHERE WE CHECK FOR THE ISSUE:**
```
[AuctionChatService] 📨 Raw WebSocket message: [Private from seller1] [Auction:123] Hello from seller
[AuctionChatService] 📋 Parsed message - Sender: seller1, Auction: 123, Content: Hello from seller
[AuctionChatService] 🔍 Available auction handlers: [123]
[AuctionChatService] ✅ Dispatching to auction 123 handler
[ChatBox] 📨 Received WebSocket message from seller1
[ChatBox] Creating message ID: seller1-Hello from seller-<timestamp>
[ChatBox] ✅ Message ID is new, adding to state
```

#### What to Check:

1. **If Backend logs show "Message sent successfully" but Browser 1 console has NO logs:**
   - WebSocket connection issue on buyer side
   - Buyer might not be connected to ChatServer
   - Check: Does `auctionChatService.connected` = true?

2. **If Browser 1 console shows auctionChatService received but NO ChatBox logs:**
   - Message not dispatched to ChatBox handler
   - Auction ID mismatch in subscription
   - Check: Does `Available auction handlers` include the auction ID?

3. **If ChatBox logs show "Skipping duplicate message":**
   - Duplicate detection blocking new message
   - Timestamp collision issue
   - Check: Message ID creation logic

4. **If all logs appear but UI doesn't update:**
   - React state update not triggering render
   - Check: Message array length in logs

### Test Case 2: Buyer to Seller Message

Follow same steps but send from Browser 1 (Buyer) to verify bidirectional works.

### Test Case 3: Database Persistence

1. Send message from seller to buyer
2. Refresh buyer's page
3. **Expected:** Messages load from database
4. **Check logs:** Should see API call to `/api/chat/messages/<auctionId>`

## Common Issues and Solutions

### Issue 1: Messages Only Appear After Refresh
**Symptoms:**
- Backend logs show "Message sent successfully"
- Database has the message
- Frontend console has NO WebSocket receive logs

**Likely Causes:**
1. Buyer's WebSocket not connected
2. Buyer not subscribed to auction before message sent
3. WebSocket connection dropped

**Solutions:**
1. Check `auctionChatService.connected` in console
2. Ensure subscription happens in `useEffect` on mount
3. Add connection state monitoring

### Issue 2: Duplicate Messages
**Symptoms:**
- Logs show "Skipping duplicate message"
- Same message appears multiple times

**Likely Causes:**
1. Message sent to both buyer and seller
2. Timestamp normalization creating same ID
3. Multiple subscriptions to same auction

**Solutions:**
- Already implemented: Normalized timestamps to second precision
- Already implemented: Set-based deduplication
- Check: Only one subscription per auction

### Issue 3: Wrong Auction Displayed
**Symptoms:**
- Message appears in wrong chat
- Auction ID mismatch in logs

**Likely Causes:**
1. Auction ID parsing error
2. Message format incorrect
3. Subscription to wrong auction

**Solutions:**
1. Verify message format: `[Private from X] [Auction:ID] content`
2. Check auction ID extraction in auctionChatService
3. Verify subscription auction ID matches message auction ID

## Debugging Commands

### Check Active WebSocket Connections
In browser console:
```javascript
// Check connection state
console.log('Connected:', window.auctionChatService?.connected);

// Check subscribed auctions
console.log('Handlers:', window.auctionChatService?.auctionHandlers);

// Force reconnect
window.auctionChatService?.connect();
```

### Check Database Messages
```bash
cd Backend-1/data
sqlite3 auction_system.db
SELECT * FROM chat_messages WHERE auction_id = 123;
```

### Check Active Chat Server Connections
Backend logs will show:
- "New client connection from: ..."
- "Active connections: N"
- "User registered: username"

## Next Steps Based on Findings

1. **If WebSocket not receiving:**
   - Add connection state indicator in UI
   - Add auto-reconnect logic
   - Check if buyer opens ChatBox before seller sends

2. **If subscription mismatch:**
   - Log exact auction ID in subscription
   - Verify auction ID type (string vs number)
   - Ensure consistent ID format

3. **If timing issue:**
   - Add message queue for offline delivery
   - Store pending messages during connection drops
   - Retry failed sends

4. **If duplicate detection too strict:**
   - Include message ID from backend
   - Use database message ID for deduplication
   - Relax timestamp precision

## Success Criteria

✅ Seller sends message  
✅ Backend logs show message saved to database  
✅ Backend logs show message sent to buyer  
✅ Buyer console shows WebSocket message received  
✅ Buyer console shows ChatBox handler called  
✅ Buyer UI shows message in real-time  
✅ Message persists after page refresh  
✅ No duplicate messages appear  

## Current Status

- ✅ Backend logging complete
- ✅ Frontend logging complete
- ✅ Database persistence working
- ✅ Message deduplication implemented
- ✅ Notification system complete
- ⏳ Real-time delivery debugging in progress
- ⏳ Waiting for test results with enhanced logs

## Files Modified for Debugging

1. `Backend-1/src/main/server/ClientHandler.java` - Added 15+ log statements
2. `Backend-1/src/main/server/ChatServer.java` - Enhanced broadcast and direct message logging
3. `Frontend-1/auction-client/src/utils/auctionChatService.js` - Added message parsing logs
4. `Frontend-1/auction-client/src/components/ChatBox.jsx` - Added state update logs
5. `Frontend-1/auction-client/src/pages/SellerChatsPage.jsx` - Added message reception logs
