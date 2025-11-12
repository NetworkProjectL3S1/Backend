package main.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import main.model.Auction;
import main.model.Auction.AuctionStatus;
import main.model.Bid;

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

        String createBidsIndex = """
            CREATE INDEX IF NOT EXISTS idx_bids_auction_id 
            ON bids(auction_id)
        """;

        String createBidsTimestampIndex = """
            CREATE INDEX IF NOT EXISTS idx_bids_timestamp 
            ON bids(timestamp DESC)
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createAuctionsTable);
            stmt.execute(createBidsTable);
            stmt.execute(createBidsIndex);
            stmt.execute(createBidsTimestampIndex);
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
        // Create auction using constructor
        Auction auction = new Auction(
            rs.getString("auction_id"),
            rs.getString("item_name"),
            rs.getString("item_description"),
            rs.getString("seller_id"),
            rs.getDouble("base_price"),
            rs.getLong("duration") / (60 * 1000), // convert ms to minutes
            rs.getString("category")
        );

        // Restore state from database
        // We need to use reflection or add setters to restore the saved state
        // For now, we'll create a new auction and manually set the bid if exists
        String highestBidder = rs.getString("current_highest_bidder");
        if (highestBidder != null && !highestBidder.isEmpty()) {
            Bid bid = new Bid(
                auction.getAuctionId(),
                highestBidder,
                rs.getDouble("current_highest_bid")
            );
            auction.placeBid(bid);
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
            rs.getDouble("amount")
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
}
