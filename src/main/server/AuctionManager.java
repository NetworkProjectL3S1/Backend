package main.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import main.model.Auction;
import main.util.DatabaseManager;

/**
 * Manages all auctions in the system
 * Enhanced to use SQL Database storage
 */
public class AuctionManager {

    // A thread-safe map to hold all active auctions in memory
    // Key: auctionId (String), Value: Auction object
    private Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    // Database manager for persistence
    private final DatabaseManager dbManager;

    // Reference to server for broadcasting
    private AuctionServer server;

    public AuctionManager() {
        this.dbManager = DatabaseManager.getInstance();
        loadAuctionsFromDatabase();
    }

    /**
     * Set server reference for broadcasting
     */
    public void setServer(AuctionServer server) {
        this.server = server;
    }

    /**
     * Load all auctions from database on startup
     */
    private void loadAuctionsFromDatabase() {
        System.out.println("[AuctionManager] Loading auctions from database...");
        Map<String, Auction> loadedAuctions = dbManager.loadAllAuctions();

        // Filter out expired auctions and update their status
        for (Auction auction : loadedAuctions.values()) {
            if (auction.hasExpired() && auction.getStatus() == Auction.AuctionStatus.ACTIVE) {
                auction.setStatus(Auction.AuctionStatus.CLOSED);
                dbManager.updateAuctionStatus(auction.getAuctionId(), Auction.AuctionStatus.CLOSED);
            }
            activeAuctions.put(auction.getAuctionId(), auction);
        }

        System.out.println("[AuctionManager] Loaded " + activeAuctions.size() + " auctions from database");
    }

    /**
     * Create a new auction with full parameters
     */
    public Auction createAuction(String itemName, String itemDescription,
            String sellerId, double basePrice,
            long durationMinutes, String category) {
        
        // Create auction in database
        Auction auction = dbManager.createAuction(
            itemName,
            itemDescription,
            sellerId,
            basePrice,
            durationMinutes,
            category
        );

        if (auction != null) {
            // Add to active auctions in memory
            activeAuctions.put(auction.getAuctionId(), auction);

            System.out.println("[AuctionManager] New auction created: " + auction.getAuctionId() +
                    " - " + itemName + " by " + sellerId);

            // Broadcast to all connected clients (if server is set)
            if (server != null) {
                server.broadcastNewAuction(auction);
            }
        }

        return auction;
    }

    /**
     * Legacy method for backward compatibility
     */
    public void createAuction(Auction auction) {
        if (auction == null)
            return;

        // Save to database
        dbManager.saveAuction(auction);
        
        // Add to memory
        activeAuctions.put(auction.getAuctionId(), auction);

        System.out.println("[AuctionManager] Auction created: " + auction.getAuctionId());

        if (server != null) {
            server.broadcastNewAuction(auction);
        }
    }

    /**
     * Get a specific auction by ID
     */
    public Auction getAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);

        // Check if auction has expired
        if (auction != null && auction.hasExpired() &&
                auction.getStatus() == Auction.AuctionStatus.ACTIVE) {
            auction.setStatus(Auction.AuctionStatus.CLOSED);
            dbManager.updateAuctionStatus(auctionId, Auction.AuctionStatus.CLOSED);
        }

        return auction;
    }

    /**
     * Get all active auctions
     */
    public Collection<Auction> getActiveAuctions() {
        return activeAuctions.values().stream()
                .filter(a -> a.getStatus() == Auction.AuctionStatus.ACTIVE && !a.hasExpired())
                .collect(Collectors.toList());
    }

    /**
     * Get all auctions (including closed/cancelled)
     */
    public Collection<Auction> getAllAuctions() {
        return activeAuctions.values();
    }

    /**
     * Get auctions by category
     */
    public Collection<Auction> getAuctionsByCategory(String category) {
        return activeAuctions.values().stream()
                .filter(a -> a.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /**
     * Get auctions by seller
     */
    public Collection<Auction> getAuctionsBySeller(String sellerId) {
        return activeAuctions.values().stream()
                .filter(a -> a.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    /**
     * Close an auction manually
     */
    public boolean closeAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && auction.getStatus() == Auction.AuctionStatus.ACTIVE) {
            auction.setStatus(Auction.AuctionStatus.CLOSED);
            dbManager.updateAuctionStatus(auctionId, Auction.AuctionStatus.CLOSED);
            System.out.println("[AuctionManager] Auction closed: " + auctionId);
            return true;
        }
        return false;
    }

    /**
     * Cancel an auction
     */
    public boolean cancelAuction(String auctionId, String requesterId) {
        Auction auction = activeAuctions.get(auctionId);

        // Only seller can cancel their auction
        if (auction != null && auction.getSellerId().equals(requesterId)) {
            auction.setStatus(Auction.AuctionStatus.CANCELLED);
            dbManager.updateAuctionStatus(auctionId, Auction.AuctionStatus.CANCELLED);
            System.out.println("[AuctionManager] Auction cancelled: " + auctionId);
            return true;
        }
        return false;
    }

    /**
     * Save all auctions to database
     */
    public void saveAllAuctions() {
        for (Auction auction : activeAuctions.values()) {
            dbManager.saveAuction(auction);
        }
        System.out.println("[AuctionManager] All auctions saved to database");
    }

    /**
     * Create a backup of the database
     */
    public void createBackup() {
        String backupPath = "data/backups/auction_backup_" + System.currentTimeMillis() + ".db";
        dbManager.backupDatabase(backupPath);
    }

    /**
     * Print statistics about auctions
     */
    public void printStatistics() {
        int total = activeAuctions.size();
        long active = activeAuctions.values().stream()
                .filter(a -> a.getStatus() == Auction.AuctionStatus.ACTIVE && !a.hasExpired())
                .count();
        long closed = activeAuctions.values().stream()
                .filter(a -> a.getStatus() == Auction.AuctionStatus.CLOSED)
                .count();
        long cancelled = activeAuctions.values().stream()
                .filter(a -> a.getStatus() == Auction.AuctionStatus.CANCELLED)
                .count();

        System.out.println("\n=== AUCTION MANAGER STATISTICS ===");
        System.out.println("Total Auctions: " + total);
        System.out.println("Active: " + active);
        System.out.println("Closed: " + closed);
        System.out.println("Cancelled: " + cancelled);
        System.out.println("===================================\n");
    }

    /**
     * Removes a specific ClientHandler from the watcher list of ALL auctions.
     * This is used for cleanup when a client disconnects.
     */
    public void removeWatcherFromAllAuctions(AuctionClientHandler handler) {
        for (Auction auction : activeAuctions.values()) {
            auction.removeWatcher(handler);
        }
    }
}
