# SQLite Database Quick Reference

## Database Location
```
data/auction_system.db
```

## Opening the Database

### Command Line
```bash
sqlite3 data/auction_system.db
```

### Using Makefile
```bash
make db-shell
```

## Useful SQL Queries

### View All Tables
```sql
.tables
```

### View Table Schema
```sql
.schema auctions
.schema bids
```

### Enable Column Headers and Better Formatting
```sql
.headers on
.mode column
```

### View All Auctions
```sql
SELECT * FROM auctions;
```

### View Active Auctions Only
```sql
SELECT auction_id, item_name, base_price, current_highest_bid, status
FROM auctions
WHERE status = 'ACTIVE'
ORDER BY created_time DESC;
```

### View Closed Auctions
```sql
SELECT auction_id, item_name, current_highest_bid, current_highest_bidder
FROM auctions
WHERE status = 'CLOSED'
ORDER BY end_time DESC;
```

### View All Bids for a Specific Auction
```sql
SELECT user_id, amount, datetime(timestamp/1000, 'unixepoch') as bid_time
FROM bids
WHERE auction_id = 'AUC001'
ORDER BY timestamp DESC;
```

### View Bidding History (Recent First)
```sql
SELECT 
    b.auction_id,
    a.item_name,
    b.user_id,
    b.amount,
    datetime(b.timestamp/1000, 'unixepoch') as bid_time
FROM bids b
JOIN auctions a ON b.auction_id = a.auction_id
ORDER BY b.timestamp DESC
LIMIT 20;
```

### Find Top Bidders
```sql
SELECT 
    user_id,
    COUNT(*) as total_bids,
    MAX(amount) as highest_bid,
    AVG(amount) as avg_bid
FROM bids
GROUP BY user_id
ORDER BY total_bids DESC;
```

### Auction Statistics by Status
```sql
SELECT 
    status,
    COUNT(*) as count,
    AVG(current_highest_bid) as avg_price,
    MAX(current_highest_bid) as max_price,
    MIN(current_highest_bid) as min_price
FROM auctions
GROUP BY status;
```

### Most Popular Auctions (by bid count)
```sql
SELECT 
    a.auction_id,
    a.item_name,
    a.current_highest_bid,
    COUNT(b.bid_id) as bid_count
FROM auctions a
LEFT JOIN bids b ON a.auction_id = b.auction_id
GROUP BY a.auction_id
ORDER BY bid_count DESC
LIMIT 10;
```

### Auctions by Category
```sql
SELECT 
    category,
    COUNT(*) as auction_count,
    AVG(current_highest_bid) as avg_price
FROM auctions
GROUP BY category
ORDER BY auction_count DESC;
```

### User's Bidding Activity
```sql
SELECT 
    a.auction_id,
    a.item_name,
    b.amount,
    datetime(b.timestamp/1000, 'unixepoch') as bid_time,
    CASE 
        WHEN a.current_highest_bidder = b.user_id THEN 'WINNING'
        ELSE 'OUTBID'
    END as bid_status
FROM bids b
JOIN auctions a ON b.auction_id = a.auction_id
WHERE b.user_id = 'buyer001'
ORDER BY b.timestamp DESC;
```

### Auctions Ending Soon (Active)
```sql
SELECT 
    auction_id,
    item_name,
    current_highest_bid,
    datetime(end_time/1000, 'unixepoch') as end_time,
    (end_time - strftime('%s', 'now')*1000)/1000 as seconds_remaining
FROM auctions
WHERE status = 'ACTIVE' 
    AND end_time > strftime('%s', 'now')*1000
ORDER BY end_time ASC
LIMIT 10;
```

### Revenue by Seller
```sql
SELECT 
    seller_id,
    COUNT(*) as auctions_sold,
    SUM(current_highest_bid) as total_revenue,
    AVG(current_highest_bid) as avg_sale_price
FROM auctions
WHERE status = 'CLOSED' 
    AND current_highest_bidder IS NOT NULL
GROUP BY seller_id
ORDER BY total_revenue DESC;
```

### Bid Competition Analysis
```sql
SELECT 
    a.auction_id,
    a.item_name,
    COUNT(DISTINCT b.user_id) as unique_bidders,
    COUNT(b.bid_id) as total_bids,
    MIN(b.amount) as first_bid,
    MAX(b.amount) as final_bid,
    (MAX(b.amount) - MIN(b.amount)) as price_increase
FROM auctions a
JOIN bids b ON a.auction_id = b.auction_id
GROUP BY a.auction_id
ORDER BY unique_bidders DESC, total_bids DESC;
```

### Recent Database Activity
```sql
SELECT 
    'Auction' as type,
    auction_id as id,
    item_name as name,
    updated_at as activity_time
FROM auctions
UNION ALL
SELECT 
    'Bid' as type,
    auction_id as id,
    user_id as name,
    created_at as activity_time
FROM bids
ORDER BY activity_time DESC
LIMIT 20;
```

## Data Management Queries

### Delete Old Closed Auctions (older than 30 days)
```sql
DELETE FROM auctions
WHERE status = 'CLOSED'
    AND end_time < strftime('%s', 'now', '-30 days')*1000;
```

### Clean Up Orphaned Bids (bids without auctions)
```sql
DELETE FROM bids
WHERE auction_id NOT IN (SELECT auction_id FROM auctions);
```

### Update All Expired Auctions to CLOSED
```sql
UPDATE auctions
SET status = 'CLOSED',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'ACTIVE'
    AND end_time < strftime('%s', 'now')*1000;
```

### Vacuum Database (optimize and reclaim space)
```sql
VACUUM;
```

## Export Data

### Export to CSV
```sql
.mode csv
.output auctions.csv
SELECT * FROM auctions;
.output stdout
```

### Export to JSON
```sql
.mode json
.output auctions.json
SELECT * FROM auctions;
.output stdout
```

## Database Maintenance

### Check Database Integrity
```sql
PRAGMA integrity_check;
```

### View Database Statistics
```sql
SELECT 
    (SELECT COUNT(*) FROM auctions) as total_auctions,
    (SELECT COUNT(*) FROM auctions WHERE status='ACTIVE') as active_auctions,
    (SELECT COUNT(*) FROM bids) as total_bids,
    (SELECT COUNT(DISTINCT user_id) FROM bids) as unique_bidders;
```

### Database File Size
```bash
ls -lh data/auction_system.db
```

## Backup Database

### Command Line
```bash
sqlite3 data/auction_system.db ".backup data/backups/backup_$(date +%Y%m%d).db"
```

### Using Makefile
```bash
make backup-db
```

## Tips

1. Always use `.headers on` and `.mode column` for better readability
2. Use LIMIT to prevent large result sets
3. Regular VACUUM helps maintain performance
4. Back up before running DELETE or UPDATE queries
5. Use transactions for bulk operations
6. Index is automatically created on `auction_id` in bids table
