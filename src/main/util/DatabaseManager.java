package main.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import main.model.Auction;
import main.model.Auction.AuctionStatus;
import main.model.Bid;
import main.model.User;

/**
 * SQLite Database Manager for Auctions and Bids
 * Provides persistent storage using SQLite database
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/auction_system.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize database connection and create tables if they don't exist
     */
    private void initializeDatabase() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Create data directory if it doesn't exist
            java.io.File dataDir = new java.io.File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // Establish connection
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DatabaseManager] Connected to SQLite database");

            // Create tables
            createTables();
            
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseManager] SQLite JDBC driver not found: " + e.getMessage());
            System.err.println("[DatabaseManager] Please add sqlite-jdbc jar to your classpath");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Database initialization failed: " + e.getMessage());
        }
    }

    /**
     * Create database tables for auctions and bids
     */
    private void createTables() throws SQLException {
        String createAuctionsTable = """
            CREATE TABLE IF NOT EXISTS auctions (
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
        """;

        String createBidsTable = """
            CREATE TABLE IF NOT EXISTS bids (
                bid_id INTEGER PRIMARY KEY AUTOINCREMENT,
                auction_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                amount REAL NOT NULL,
                timestamp INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (auction_id) REFERENCES auctions(auction_id)
            )
        """;
        
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                email TEXT,
                role TEXT NOT NULL DEFAULT 'BUYER',
                token TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createChatMessagesTable = """
            CREATE TABLE IF NOT EXISTS chat_messages (
                message_id INTEGER PRIMARY KEY AUTOINCREMENT,
                auction_id TEXT NOT NULL,
                sender_username TEXT NOT NULL,
                recipient_username TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                is_read INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (auction_id) REFERENCES auctions(auction_id)
            )
        """;
        
        String createNotificationsTable = """
            CREATE TABLE IF NOT EXISTS notifications (
                notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                auction_id TEXT,
                timestamp INTEGER NOT NULL,
                is_read INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (auction_id) REFERENCES auctions(auction_id)
            )
        """;

        String createBidsIndex = """
            CREATE INDEX IF NOT EXISTS idx_bids_auction_id 
            ON bids(auction_id)
        """;

        String createBidsTimestampIndex = """
            CREATE INDEX IF NOT EXISTS idx_bids_timestamp 
            ON bids(timestamp DESC)
        """;
        
        String createUsersUsernameIndex = """
            CREATE INDEX IF NOT EXISTS idx_users_username 
            ON users(username)
        """;
        
        String createUsersTokenIndex = """
            CREATE INDEX IF NOT EXISTS idx_users_token 
            ON users(token)
        """;
        
        String createChatMessagesAuctionIndex = """
            CREATE INDEX IF NOT EXISTS idx_chat_messages_auction_id 
            ON chat_messages(auction_id)
        """;
        
        String createChatMessagesUsersIndex = """
            CREATE INDEX IF NOT EXISTS idx_chat_messages_users 
            ON chat_messages(sender_username, recipient_username)
        """;
        
        String createNotificationsUsernameIndex = """
            CREATE INDEX IF NOT EXISTS idx_notifications_username 
            ON notifications(username, is_read)
        """;
        
        String createNotificationsTimestampIndex = """
            CREATE INDEX IF NOT EXISTS idx_notifications_timestamp 
            ON notifications(timestamp DESC)
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createAuctionsTable);
            stmt.execute(createBidsTable);
            stmt.execute(createUsersTable);
            stmt.execute(createChatMessagesTable);
            stmt.execute(createNotificationsTable);
            stmt.execute(createBidsIndex);
            stmt.execute(createBidsTimestampIndex);
            stmt.execute(createUsersUsernameIndex);
            stmt.execute(createUsersTokenIndex);
            stmt.execute(createChatMessagesAuctionIndex);
            stmt.execute(createChatMessagesUsersIndex);
            stmt.execute(createNotificationsUsernameIndex);
            stmt.execute(createNotificationsTimestampIndex);
            System.out.println("[DatabaseManager] Database tables created successfully");
        }
    }

    /**
     * Save an auction to the database
     */
    public synchronized boolean saveAuction(Auction auction) {
        if (auction == null) {
            return false;
        }

        String sql = """
            INSERT OR REPLACE INTO auctions 
            (auction_id, item_name, item_description, seller_id, base_price, 
             current_highest_bid, current_highest_bidder, created_time, end_time, 
             duration, status, category, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auction.getAuctionId());
            pstmt.setString(2, auction.getItemName());
            pstmt.setString(3, auction.getItemDescription());
            pstmt.setString(4, auction.getSellerId());
            pstmt.setDouble(5, auction.getBasePrice());
            pstmt.setDouble(6, auction.getCurrentHighestBid());
            pstmt.setString(7, auction.getCurrentHighestBidder());
            pstmt.setLong(8, auction.getCreatedTime());
            pstmt.setLong(9, auction.getEndTime());
            pstmt.setLong(10, auction.getDuration());
            pstmt.setString(11, auction.getStatus().name());
            pstmt.setString(12, auction.getCategory());

            pstmt.executeUpdate();
            System.out.println("[DatabaseManager] Auction saved: " + auction.getAuctionId());
            return true;

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to save auction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load an auction by ID
     */
    public Auction loadAuction(String auctionId) {
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return createAuctionFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to load auction: " + e.getMessage());
        }
        return null;
    }

    /**
     * Load all auctions from the database
     */
    public Map<String, Auction> loadAllAuctions() {
        Map<String, Auction> auctions = new ConcurrentHashMap<>();
        String sql = "SELECT * FROM auctions ORDER BY created_time DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Auction auction = createAuctionFromResultSet(rs);
                auctions.put(auction.getAuctionId(), auction);
            }
            System.out.println("[DatabaseManager] Loaded " + auctions.size() + " auctions");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to load auctions: " + e.getMessage());
        }
        return auctions;
    }

    /**
     * Load auctions by status
     */
    public List<Auction> loadAuctionsByStatus(AuctionStatus status) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY created_time DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                auctions.add(createAuctionFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to load auctions by status: " + e.getMessage());
        }
        return auctions;
    }

    /**
     * Load auctions by seller
     */
    public List<Auction> loadAuctionsBySeller(String sellerId) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE seller_id = ? ORDER BY created_time DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sellerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                auctions.add(createAuctionFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to load auctions by seller: " + e.getMessage());
        }
        return auctions;
    }

    /**
     * Save a bid to the database
     */
    public synchronized boolean saveBid(Bid bid) {
        if (bid == null) {
            return false;
        }

        String sql = """
            INSERT INTO bids (auction_id, user_id, amount, timestamp) 
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bid.getAuctionId());
            pstmt.setString(2, bid.getUserId());
            pstmt.setDouble(3, bid.getAmount());
            pstmt.setLong(4, bid.getTimestamp());

            pstmt.executeUpdate();
            System.out.println("[DatabaseManager] Bid saved: " + bid.getUserId() + 
                             " bid " + bid.getAmount() + " on " + bid.getAuctionId());
            return true;

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to save bid: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Save bid and update auction in a single transaction
     * Ensures atomicity - either both operations succeed or both fail
     */
    public synchronized boolean saveBidWithTransaction(Bid bid, Auction auction) {
        if (bid == null || auction == null) {
            return false;
        }
        
        try {
            // Update auction object with new bid BEFORE saving to database
            boolean bidAccepted = auction.placeBid(bid);
            if (!bidAccepted) {
                System.err.println("[DatabaseManager] Bid was not accepted by auction logic");
                return false;
            }
            
            // Disable auto-commit to start transaction
            connection.setAutoCommit(false);
            
            // Save bid
            String bidSql = """
                INSERT INTO bids (auction_id, user_id, amount, timestamp) 
                VALUES (?, ?, ?, ?)
            """;
            
            try (PreparedStatement bidStmt = connection.prepareStatement(bidSql)) {
                bidStmt.setString(1, bid.getAuctionId());
                bidStmt.setString(2, bid.getUserId());
                bidStmt.setDouble(3, bid.getAmount());
                bidStmt.setLong(4, bid.getTimestamp());
                bidStmt.executeUpdate();
            }
            
            // Update auction with new highest bid and bidder
            String auctionSql = """
                INSERT OR REPLACE INTO auctions 
                (auction_id, item_name, item_description, seller_id, base_price, 
                 current_highest_bid, current_highest_bidder, status, category, 
                 created_time, end_time, duration)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement auctionStmt = connection.prepareStatement(auctionSql)) {
                auctionStmt.setString(1, auction.getAuctionId());
                auctionStmt.setString(2, auction.getItemName());
                auctionStmt.setString(3, auction.getItemDescription());
                auctionStmt.setString(4, auction.getSellerId());
                auctionStmt.setDouble(5, auction.getBasePrice());
                auctionStmt.setDouble(6, auction.getCurrentHighestBid());
                auctionStmt.setString(7, auction.getCurrentHighestBidder());
                auctionStmt.setString(8, auction.getStatus().name());
                auctionStmt.setString(9, auction.getCategory());
                auctionStmt.setLong(10, auction.getCreatedTime());
                auctionStmt.setLong(11, auction.getEndTime());
                auctionStmt.setLong(12, auction.getDuration());
                
                System.out.println("[DatabaseManager] Saving auction to DB:");
                System.out.println("[DatabaseManager]   Auction ID: " + auction.getAuctionId());
                System.out.println("[DatabaseManager]   Current Highest Bid: $" + auction.getCurrentHighestBid());
                System.out.println("[DatabaseManager]   Current Highest Bidder: " + auction.getCurrentHighestBidder());
                
                auctionStmt.executeUpdate();
            }
            
            // Commit transaction
            connection.commit();
            
            System.out.println("[DatabaseManager] Transaction committed: Bid saved and auction updated for " + 
                             auction.getAuctionId() + " - New highest bidder: " + auction.getCurrentHighestBidder() +
                             " with bid: $" + auction.getCurrentHighestBid());
            return true;
            
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Transaction failed, rolling back: " + e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("[DatabaseManager] Rollback failed: " + rollbackEx.getMessage());
            }
            return false;
            
        } finally {
            try {
                // Re-enable auto-commit
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    /**
     * Load all bids for a specific auction
     */
    public List<Bid> loadBidsByAuction(String auctionId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY timestamp DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bids.add(createBidFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to load bids: " + e.getMessage());
        }
        return bids;
    }

    /**
     * Get all bids for a specific auction (alias for loadBidsByAuction for API compatibility)
     */
    public List<Bid> getBidsByAuction(String auctionId) {
        return loadBidsByAuction(auctionId);
    }

    /**
     * Load all bids by a specific user
     */
    public List<Bid> loadBidsByUser(String userId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE user_id = ? ORDER BY timestamp DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bids.add(createBidFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to load user bids: " + e.getMessage());
        }
        return bids;
    }

    /**
     * Get bid history for an auction (recent first)
     */
    public List<Bid> getBidHistory(String auctionId, int limit) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bids.add(createBidFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to get bid history: " + e.getMessage());
        }
        return bids;
    }

    /**
     * Delete an auction (and cascade delete its bids)
     */
    public synchronized boolean deleteAuction(String auctionId) {
        try {
            connection.setAutoCommit(false);

            // Delete bids first
            String deleteBids = "DELETE FROM bids WHERE auction_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteBids)) {
                pstmt.setString(1, auctionId);
                pstmt.executeUpdate();
            }

            // Delete auction
            String deleteAuction = "DELETE FROM auctions WHERE auction_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteAuction)) {
                pstmt.setString(1, auctionId);
                pstmt.executeUpdate();
            }

            connection.commit();
            System.out.println("[DatabaseManager] Auction deleted: " + auctionId);
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("[DatabaseManager] Rollback failed: " + ex.getMessage());
            }
            System.err.println("[DatabaseManager] Failed to delete auction: " + e.getMessage());
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    /**
     * Update auction status
     */
    public synchronized boolean updateAuctionStatus(String auctionId, AuctionStatus status) {
        String sql = "UPDATE auctions SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE auction_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, auctionId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[DatabaseManager] Auction status updated: " + auctionId + " -> " + status);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to update auction status: " + e.getMessage());
        }
        return false;
    }

    /**
     * Update entire auction object
     */
    public synchronized boolean updateAuction(Auction auction) {
        return saveAuction(auction); // Uses INSERT OR REPLACE
    }

    /**
     * Get a specific auction by ID (alias for loadAuction for API compatibility)
     */
    public Auction getAuction(String auctionId) {
        return loadAuction(auctionId);
    }

    /**
     * Get all auctions as a collection
     */
    public Collection<Auction> getAllAuctions() {
        return loadAllAuctions().values();
    }

    /**
     * Create a new auction with auto-generated ID
     */
    public synchronized Auction createAuction(String itemName, String itemDescription,
            String sellerId, double basePrice, long durationMinutes, String category) {
        
        // Generate unique auction ID
        String auctionId = generateAuctionId();
        
        // Create auction object
        Auction auction = new Auction(
            auctionId,
            itemName,
            itemDescription,
            sellerId,
            basePrice,
            durationMinutes,
            category
        );
        
        // Save to database
        if (saveAuction(auction)) {
            System.out.println("[DatabaseManager] New auction created: " + auctionId);
            return auction;
        }
        
        return null;
    }

    /**
     * Generate unique auction ID
     */
    private synchronized String generateAuctionId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return "auction-" + timestamp + "-" + random;
    }

    /**
     * Get auction count by status
     */
    public int getAuctionCount(AuctionStatus status) {
        String sql = "SELECT COUNT(*) FROM auctions WHERE status = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to get auction count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get total bid count for an auction
     */
    public int getBidCount(String auctionId) {
        String sql = "SELECT COUNT(*) FROM bids WHERE auction_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to get bid count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Helper method to create Auction object from ResultSet
     */
    private Auction createAuctionFromResultSet(ResultSet rs) throws SQLException {
        // Create auction using constructor with preserved timestamps
        Auction auction = new Auction(
            rs.getString("auction_id"),
            rs.getString("item_name"),
            rs.getString("item_description"),
            rs.getString("seller_id"),
            rs.getDouble("base_price"),
            rs.getLong("created_time"), // Preserve original created time
            rs.getLong("end_time"), // Preserve original end time
            rs.getLong("duration"),
            rs.getString("category")
        );

        // Restore state from database
        // We need to use reflection or add setters to restore the saved state
        // For now, we'll create a new auction and manually set the bid if exists
        String highestBidder = rs.getString("current_highest_bidder");
        double highestBid = rs.getDouble("current_highest_bid");
        
        System.out.println("[DatabaseManager] Loading auction from DB:");
        System.out.println("[DatabaseManager]   Auction ID: " + auction.getAuctionId());
        System.out.println("[DatabaseManager]   Item: " + auction.getItemName());
        System.out.println("[DatabaseManager]   Highest Bidder from DB: " + highestBidder);
        System.out.println("[DatabaseManager]   Highest Bid from DB: $" + highestBid);
        
        if (highestBidder != null && !highestBidder.isEmpty()) {
            Bid bid = new Bid(
                auction.getAuctionId(),
                highestBidder,
                highestBid
            );
            auction.placeBid(bid);
            System.out.println("[DatabaseManager]   ✅ Bid restored to auction object");
        } else {
            System.out.println("[DatabaseManager]   ⚠️ No bidder found in database");
        }

        // Set status
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));

        return auction;
    }

    /**
     * Helper method to create Bid object from ResultSet
     */
    private Bid createBidFromResultSet(ResultSet rs) throws SQLException {
        return new Bid(
            rs.getString("auction_id"),
            rs.getString("user_id"),
            rs.getDouble("amount"),
            rs.getLong("timestamp")
        );
    }

    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DatabaseManager] Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to close connection: " + e.getMessage());
        }
    }

    /**
     * Backup database to a file
     */
    public boolean backupDatabase(String backupPath) {
        String sql = "VACUUM INTO ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, backupPath);
            pstmt.execute();
            System.out.println("[DatabaseManager] Database backed up to: " + backupPath);
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Backup failed: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== User Authentication Methods ====================
    
    /**
     * Register a new user
     */
    public synchronized boolean registerUser(String username, String password, String email, String role, String token) {
        String sql = """
            INSERT INTO users (username, password, email, role, token, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            pstmt.setString(3, email);
            pstmt.setString(4, role);
            pstmt.setString(5, token);
            
            pstmt.executeUpdate();
            System.out.println("[DatabaseManager] User registered: " + username);
            return true;
            
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to register user: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Authenticate user with username and password
     */
    public User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password");
                if (verifyPassword(password, storedHash)) {
                    return createUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Authentication error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get user by token
     */
    public User getUserByToken(String token) {
        String sql = "SELECT * FROM users WHERE token = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, token);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return createUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to get user by token: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Check if username exists
     */
    public boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to check user existence: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Update user token
     */
    public synchronized boolean updateUserToken(String username, String token) {
        String sql = "UPDATE users SET token = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, token);
            pstmt.setString(2, username);
            
            pstmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to update token: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update user email
     */
    public synchronized boolean updateUserEmail(String username, String email) {
        String sql = "UPDATE users SET email = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, username);
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("[DatabaseManager] Email updated for user: " + username);
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to update email: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update user password
     */
    public synchronized boolean updateUserPassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hashPassword(newPassword));
            pstmt.setString(2, username);
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("[DatabaseManager] Password updated for user: " + username);
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Failed to update password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create User object from ResultSet
     */
    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        return new User(
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("email"),
            rs.getString("role"),
            rs.getString("token")
        );
    }
    
    // ========================
    // Chat Messages Methods
    // ========================
    
    /**
     * Save a chat message to the database
     */
    public synchronized boolean saveChatMessage(main.model.ChatMessage message) {
        String sql = """
            INSERT INTO chat_messages (auction_id, sender_username, recipient_username, 
                                      content, timestamp, is_read)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, message.getAuctionId());
            pstmt.setString(2, message.getSenderUsername());
            pstmt.setString(3, message.getRecipientUsername());
            pstmt.setString(4, message.getContent());
            pstmt.setLong(5, message.getTimestamp());
            pstmt.setInt(6, message.isRead() ? 1 : 0);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[DatabaseManager] Chat message saved for auction: " + message.getAuctionId());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error saving chat message: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get all chat messages for a specific auction
     */
    public synchronized List<main.model.ChatMessage> getChatMessagesByAuction(String auctionId) {
        List<main.model.ChatMessage> messages = new ArrayList<>();
        String sql = """
            SELECT * FROM chat_messages 
            WHERE auction_id = ? 
            ORDER BY timestamp ASC
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(createChatMessageFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error fetching chat messages: " + e.getMessage());
        }
        
        return messages;
    }
    
    /**
     * Get unique buyers who have sent messages for a seller's auctions
     */
    public synchronized Map<String, List<String>> getBuyersBySeller(String sellerUsername) {
        Map<String, List<String>> auctionBuyers = new ConcurrentHashMap<>();
        String sql = """
            SELECT DISTINCT cm.auction_id, cm.sender_username 
            FROM chat_messages cm
            JOIN auctions a ON cm.auction_id = a.auction_id
            WHERE a.seller_id = ? AND cm.sender_username != ?
            ORDER BY cm.auction_id, cm.timestamp DESC
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sellerUsername);
            pstmt.setString(2, sellerUsername);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String auctionId = rs.getString("auction_id");
                String buyer = rs.getString("sender_username");
                
                auctionBuyers.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(buyer);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error fetching buyers: " + e.getMessage());
        }
        
        return auctionBuyers;
    }
    
    /**
     * Get unread message count for a specific auction and user
     */
    public synchronized int getUnreadMessageCount(String auctionId, String username) {
        String sql = """
            SELECT COUNT(*) as count FROM chat_messages 
            WHERE auction_id = ? AND recipient_username = ? AND is_read = 0
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error getting unread count: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Mark messages as read
     */
    public synchronized boolean markMessagesAsRead(String auctionId, String username) {
        String sql = """
            UPDATE chat_messages 
            SET is_read = 1 
            WHERE auction_id = ? AND recipient_username = ? AND is_read = 0
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error marking messages as read: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Create ChatMessage object from ResultSet
     */
    private main.model.ChatMessage createChatMessageFromResultSet(ResultSet rs) throws SQLException {
        main.model.ChatMessage message = new main.model.ChatMessage();
        message.setMessageId(rs.getLong("message_id"));
        message.setAuctionId(rs.getString("auction_id"));
        message.setSenderUsername(rs.getString("sender_username"));
        message.setRecipientUsername(rs.getString("recipient_username"));
        message.setContent(rs.getString("content"));
        message.setTimestamp(rs.getLong("timestamp"));
        message.setRead(rs.getInt("is_read") == 1);
        return message;
    }
    
    /**
     * Save a notification to the database
     */
    public synchronized boolean saveNotification(main.model.Notification notification) {
        String sql = """
            INSERT INTO notifications 
            (username, type, title, message, auction_id, timestamp, is_read) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, notification.getUsername());
            pstmt.setString(2, notification.getType());
            pstmt.setString(3, notification.getTitle());
            pstmt.setString(4, notification.getMessage());
            pstmt.setString(5, notification.getAuctionId());
            pstmt.setLong(6, notification.getTimestamp());
            pstmt.setInt(7, notification.isRead() ? 1 : 0);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error saving notification: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get notifications for a user
     */
    public synchronized List<main.model.Notification> getNotifications(String username) {
        List<main.model.Notification> notifications = new ArrayList<>();
        String sql = """
            SELECT * FROM notifications 
            WHERE username = ? 
            ORDER BY timestamp DESC
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(createNotificationFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error getting notifications: " + e.getMessage());
        }
        
        return notifications;
    }
    
    /**
     * Get unread notification count for a user
     */
    public synchronized int getUnreadNotificationCount(String username) {
        String sql = """
            SELECT COUNT(*) as count FROM notifications 
            WHERE username = ? AND is_read = 0
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error getting unread notification count: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Mark a notification as read
     */
    public synchronized boolean markNotificationAsRead(long notificationId) {
        String sql = """
            UPDATE notifications 
            SET is_read = 1 
            WHERE notification_id = ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, notificationId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error marking notification as read: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Mark all notifications as read for a user
     */
    public synchronized boolean markAllNotificationsAsRead(String username) {
        String sql = """
            UPDATE notifications 
            SET is_read = 1 
            WHERE username = ? AND is_read = 0
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error marking all notifications as read: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Create Notification object from ResultSet
     */
    private main.model.Notification createNotificationFromResultSet(ResultSet rs) throws SQLException {
        main.model.Notification notification = new main.model.Notification();
        notification.setNotificationId(rs.getLong("notification_id"));
        notification.setUsername(rs.getString("username"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setAuctionId(rs.getString("auction_id"));
        notification.setTimestamp(rs.getLong("timestamp"));
        notification.setRead(rs.getInt("is_read") == 1);
        return notification;
    }
    
    /**
     * Hash password (simple implementation - use BCrypt in production)
```
     */
    private String hashPassword(String password) {
        // Simple hash for now - in production, use BCrypt or similar
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Hashing error: " + e.getMessage());
            return password; // Fallback (not secure)
        }
    }
    
    /**
     * Verify password against hash
     */
    private boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }
}
