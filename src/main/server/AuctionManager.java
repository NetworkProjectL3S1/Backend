package main.server;

import main.model.Auction;
import main.util.AuctionFileStorage;
import java.util.Map;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages all auctions in the system
 * Enhanced by Module 2: Auction Creation
 */
public class AuctionManager {

    // A thread-safe map to hold all active auctions
    // Key: auctionId (String), Value: Auction object
    private Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    // File storage for persistence
    private final AuctionFileStorage fileStorage;

    // Auction ID counter
    private final AtomicInteger auctionCounter = new AtomicInteger(1);

    // Reference to server for broadcasting
    private AuctionServer server;

    public AuctionManager() {
        this.fileStorage = AuctionFileStorage.getInstance();
        loadAuctionsFromStorage();
    }

    /**
     * Set server reference for broadcasting
     */
    public void setServer(AuctionServer server) {
        this.server = server;
    }

    /**
     * Load all auctions from persistent storage on startup
     */
    private void loadAuctionsFromStorage() {
        System.out.println("[AuctionManager] Loading auctions from storage...");
        Map<String, Auction> loadedAuctions = fileStorage.loadAllAuctions();

        // Filter out expired auctions and update their status
        for (Auction auction : loadedAuctions.values()) {
            if (auction.hasExpired() && auction.getStatus() == Auction.AuctionStatus.ACTIVE) {
                auction.setStatus(Auction.AuctionStatus.CLOSED);
                fileStorage.saveAuction(auction); // Update storage
            }
            activeAuctions.put(auction.getAuctionId(), auction);

            // Update counter to avoid ID conflicts
            try {
                int id = Integer.parseInt(auction.getAuctionId().replace("auction-", ""));
                if (id >= auctionCounter.get()) {
                    auctionCounter.set(id + 1);
                }
            } catch (NumberFormatException e) {
                // Ignore non-numeric IDs
            }
        }

        System.out.println("[AuctionManager] Loaded " + activeAuctions.size() + " auctions");
    }

    /**
     * Create a new auction with full parameters
     * This is the main method for Module 2
     */
    public Auction createAuction(String itemName, String itemDescription,
            String sellerId, double basePrice,
            long durationMinutes, String category) {
        // Generate unique auction ID
        String auctionId = generateAuctionId();

        // Create the auction object
        Auction auction = new Auction(
                auctionId,
                itemName,
                itemDescription,
                sellerId,
                basePrice,
                durationMinutes,
                category);

        // Add to active auctions
        activeAuctions.put(auctionId, auction);

        // Persist to file storage
        fileStorage.saveAuction(auction);

        // Also export to text for easy viewing
        fileStorage.exportAuctionToText(auction);

        System.out.println("[AuctionManager] New auction created: " + auctionId +
                " - " + itemName + " by " + sellerId);

        // Broadcast to all connected clients (if server is set)
        if (server != null) {
            server.broadcastNewAuction(auction);
        }

        return auction;
    }

    /**
     * Legacy method for backward compatibility
     */
    public void createAuction(Auction auction) {
        if (auction == null)
            return;

        activeAuctions.put(auction.getAuctionId(), auction);
        fileStorage.saveAuction(auction);

        System.out.println("[AuctionManager] Auction created: " + auction.getAuctionId());

        if (server != null) {
            server.broadcastNewAuction(auction);
        }
    }

    /**
     * Generate a unique auction ID
     */
    private String generateAuctionId() {
        return "auction-" + auctionCounter.getAndIncrement();
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
            fileStorage.saveAuction(auction); // Update storage
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
            fileStorage.saveAuction(auction);
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
            fileStorage.saveAuction(auction);
            System.out.println("[AuctionManager] Auction cancelled: " + auctionId);
            return true;
        }
        return false;
    }

    /**
     * Save all auctions to storage
     */
    public void saveAllAuctions() {
        fileStorage.saveAllAuctions(activeAuctions);
    }

    /**
     * Create a backup of all auctions
     */
    public void createBackup() {
        fileStorage.createBackup();
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
