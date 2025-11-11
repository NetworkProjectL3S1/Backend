# Testing Guide: Module 2 - Auction Creation

## 🧪 Quick Testing Options

### Option 1: Automated Test Suite (Easiest - No Server Needed)

Run all tests automatically:

```bash
java -cp bin main.test.AuctionCreationTest
```

**What it tests:**

- ✅ Auction creation with all parameters
- ✅ File persistence (save/load)
- ✅ Category filtering
- ✅ Auction expiration
- ✅ Data retrieval

**Expected Output:** All 5 tests should pass with ✅

---

### Option 2: View Created Auctions

#### View text exports (human-readable):

```bash
# Windows PowerShell
Get-Content "data\auctions\auction-1.txt"

# Linux/Mac
cat data/auctions/auction-1.txt
```

#### List all auction files:

```bash
# Windows
dir data\auctions\*.txt

# Linux/Mac
ls data/auctions/*.txt
```

---

### Option 3: Interactive Client (Full Experience)

**Step 1: Start the Auction Server**

```bash
# Open Terminal 1
java -cp bin main.server.ServerMain
```

**Step 2: Start the Auction Creator Client**

```bash
# Open Terminal 2
java -cp bin main.client.AuctionCreatorClient localhost 9999
```

**Step 3: Use the Interactive Menu**

You'll see:

```
=== AUCTION CREATOR CLIENT ===
Commands:
  1. create   - Create a new auction
  2. list     - List all active auctions
  3. listcat  - List auctions by category
  4. details  - Get auction details
  5. watch    - Watch an auction
  6. bid      - Place a bid
  7. help     - Show this menu
  8. quit     - Exit
===============================

>
```

---

## 📝 Step-by-Step Testing Scenarios

### Scenario 1: Create Your First Auction

```
> create
```

Then enter:

```
Item Name: Vintage Watch
Description: Rare Rolex from 1970s
Seller ID: yourname
Base Price: $5000
Duration (minutes): 60
Category: collectibles
```

**Expected Result:**

```
✓ Auction Created Successfully!
  Auction ID: auction-XX
  Item: Vintage Watch
```

---

### Scenario 2: List All Active Auctions

```
> list
```

**Expected Result:**

```
--- all ---
NEW_AUCTION:auction-1:Vintage Camera:...:photography:...
NEW_AUCTION:auction-2:Gaming Laptop:...:electronics:...
--- End of auction list ---
```

---

### Scenario 3: Filter by Category

```
> listcat
Enter category: electronics
```

**Expected Result:**
Shows only electronics auctions

---

### Scenario 4: View Auction Details

```
> details
Enter auction ID: auction-1
```

**Expected Result:**

```
📋 AUCTION DETAILS
  ID: auction-1
  Item: Vintage Camera
  Description: Canon AE-1 from 1976
  Base Price: $250.00
  Current Bid: $250.00
  Highest Bidder: none
  Time Remaining: 5400 seconds
  Category: photography
  Status: ACTIVE
```

---

### Scenario 5: Watch and Bid on Auction

```
> watch
Enter auction ID to watch: auction-1
```

**Expected Result:**

```
✓ You are now watching auction-1
```

Then place a bid:

```
> bid
Enter auction ID: auction-1
Enter bid amount: $300
```

**Expected Result:**

```
✓ Your bid of 300.0 was accepted.
```

---

## 🔍 Verify File Persistence

### Check saved auction files:

```bash
# Windows
dir data\auctions

# Linux/Mac
ls -la data/auctions/
```

**You should see:**

- `auction-1.dat` (binary file)
- `auction-1.txt` (text export)
- `auction-2.dat`, `auction-2.txt`, etc.
- `index.dat` (index file)

### View a text export:

```bash
# Windows
type data\auctions\auction-1.txt

# Linux/Mac
cat data/auctions/auction-1.txt
```

---

## 🎯 Quick Demo Script (For Presentation)

### 1. Run Automated Tests

```bash
java -cp bin main.test.AuctionCreationTest
```

**Show:** All tests passing ✅

### 2. View Stored Data

```bash
type data\auctions\auction-1.txt
```

**Show:** Auction details saved to file

### 3. Start Interactive Client

```bash
# Terminal 1: Server
java -cp bin main.server.ServerMain

# Terminal 2: Client
java -cp bin main.client.AuctionCreatorClient localhost 9999
```

### 4. Create Live Auction

```
> create
Item Name: Gaming Console
Description: PS5 with 2 controllers
Seller ID: demo_user
Base Price: $450
Duration (minutes): 30
Category: electronics
```

### 5. List Auctions

```
> list
```

**Show:** Your new auction appears in the list

### 6. View Details

```
> details
Enter auction ID: [your-auction-id]
```

**Show:** Full auction information

---

## 📊 What to Look For (Success Indicators)

### ✅ Automated Tests:

- All 5 tests show "PASSED"
- No error messages
- Statistics show correct counts

### ✅ File Persistence:

- Files created in `data/auctions/`
- Text files are readable
- Auctions reload after restart

### ✅ Interactive Client:

- Menu appears correctly
- Can create auctions
- Real-time updates appear
- Can filter by category
- Bid placement works

### ✅ Broadcasting:

- Open 2+ clients
- Create auction in one
- See it appear in others immediately

---

## 🐛 Troubleshooting

### Problem: "Cannot find main class"

**Solution:**

```bash
# Recompile
javac -encoding UTF-8 -d bin -sourcepath src src/main/server/AuctionServer.java
javac -encoding UTF-8 -d bin -sourcepath src src/main/client/AuctionCreatorClient.java
javac -encoding UTF-8 -d bin -sourcepath src src/main/test/AuctionCreationTest.java
```

### Problem: "Connection refused"

**Solution:** Make sure server is running first in Terminal 1

### Problem: Files not saving

**Solution:** Check that `data/auctions/` directory exists (created automatically)

---

## 💡 Testing Tips

1. **Always run automated tests first** - Quick verification everything works
2. **Check text files** - Easy way to see what's stored
3. **Use multiple clients** - Test broadcasting feature
4. **Create different categories** - Test filtering
5. **Try edge cases** - Very short durations, high prices, etc.

---

## 📈 Advanced Testing

### Test Persistence Across Restarts:

```bash
# 1. Create some auctions
java -cp bin main.client.AuctionCreatorClient localhost 9999
# Create 2-3 auctions, then quit

# 2. Stop server (Ctrl+C)

# 3. Restart server
java -cp bin main.server.ServerMain

# 4. Reconnect client
java -cp bin main.client.AuctionCreatorClient localhost 9999

# 5. List auctions - should still be there!
> list
```

### Test Concurrent Creation:

```bash
# Open 3 terminals, all running:
java -cp bin main.client.AuctionCreatorClient localhost 9999

# Create auctions from different clients simultaneously
# All clients should see all new auctions
```

---

## 🎓 For Demonstration/Grading

**Recommended Demo Flow:**

1. Run automated tests (30 seconds)
2. Show file persistence (15 seconds)
3. Start interactive client (30 seconds)
4. Create 1-2 auctions live (1 minute)
5. Show listing/filtering (30 seconds)
6. Show text export files (15 seconds)

**Total Time:** ~3-4 minutes

**Key Points to Emphasize:**

- ✅ Thread-safe concurrent operations
- ✅ File I/O persistence
- ✅ Real-time broadcasting
- ✅ Clean integration with existing code
- ✅ Comprehensive testing

---

Good luck with your demonstration! 🚀
