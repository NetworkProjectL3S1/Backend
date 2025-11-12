package main.server;

import main.model.Auction;
import main.util.AuctionFileStorage;
import java.util.Map;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages all auctions in the system
 * Enhanced by Module 2: Auction Creation
 */
public class AuctionManager {

    private static AuctionManager instance;
    private AuctionServer server;
    private ScheduledExecutorService scheduler;

    // A thread-safe map to hold all active auctions
    // Key: auctionId (String), Value: Auction object
    private Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    // File storage for persistence
    private final AuctionFileStorage fileStorage;

    // Auction ID counter
    private final AtomicInteger auctionCounter = new AtomicInteger(1);


    public AuctionManager() {
        this.fileStorage = AuctionFileStorage.getInstance();
        loadAuctionsFromStorage();
        startAuctionMonitor();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
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

    // In main.server.AuctionManager.java (Add this method)

    /**
     * Handles the placement of a bid by routing it to the thread-safe Auction model.
     * This is the main method for Module 3.
     *
     * @param bidderUsername The username of the client placing the bid (from Module 1 authentication).
     * @param args Protocol Args: auctionId:amount
     * @return A response message (SUCCESS or ERROR)
     */
    public String placeBid(String bidderUsername, String args) {
        try {
            String[] params = args.split(":", 2);
            if (params.length != 2) {
                return "ERROR_BID: Invalid format. Use auctionId:amount";
            }

            String auctionId = params[0];
            double bidAmount = Double.parseDouble(params[1]);

            Auction auction = getAuction(auctionId); // getAuction handles expiration check
            if (auction == null || auction.getStatus() != Auction.AuctionStatus.ACTIVE) {
                return "ERROR_BID: Auction is not active or not found.";
            }

            // Thread-safe placement happens inside the Auction model
            boolean success = auction.placeBid(new main.model.Bid(auctionId, bidderUsername, bidAmount));

            if (success) {
                // Broadcast the new highest bid (Module 4)
                BidBroadcaster.getInstance().handleNewBid(
                        auction,
                        new main.model.Bid(auctionId, bidderUsername, bidAmount),
                        null // Sender can be null here; BidBroadcaster will handle client confirmation
                );

                // Persist the updated auction state
                fileStorage.saveAuction(auction);

                return "SUCCESS_BID: Bid of $" + bidAmount + " placed on " + auction.getItemName();
            } else {
                return "ERROR_BID: Bid amount must be higher than the current price (" + auction.getCurrentHighestBid() + ")";
            }

        } catch (NumberFormatException e) {
            return "ERROR_BID: Bid amount must be a valid number.";
        } catch (Exception e) {
            System.err.println("Error placing bid: " + e.getMessage());
            return "ERROR_BID: Internal server error while placing bid.";
        }
    }

    /**
     * Starts a scheduled task to periodically check for expired auctions.
     * This ensures automatic auction closure and highest bid determination.
     */
    private void startAuctionMonitor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // Check every 10 seconds (adjust frequency as needed)
        scheduler.scheduleAtFixedRate(this::checkAndCloseExpiredAuctions, 0, 10, TimeUnit.SECONDS);

        System.out.println("[AuctionManager] Auction closing monitor scheduled to run every 10 seconds.");
    }

    /**
     * The core task that iterates over active auctions and closes them if expired.
     */
    private void checkAndCloseExpiredAuctions() {
        System.out.println("[Monitor] Checking for expired auctions...");

        activeAuctions.values().stream()
                .filter(a -> a.getStatus() == main.model.Auction.AuctionStatus.ACTIVE && a.hasExpired())
                .forEach(this::performAuctionClosure);
    }

    /**
     * Finalizes an auction, determines the winner, and notifies clients.
     */
    private void performAuctionClosure(main.model.Auction auction) {
        synchronized (auction) {
            if (auction.getStatus() != main.model.Auction.AuctionStatus.ACTIVE) {
                return; // Already handled
            }

            auction.setStatus(main.model.Auction.AuctionStatus.CLOSED);
            fileStorage.saveAuction(auction); // Persist the closed status

            System.out.println("[Monitor] Auction closed: " + auction.getAuctionId());

            String winner = auction.getCurrentHighestBidder();
            double finalPrice = auction.getCurrentHighestBid();

            String closureMessage;
            if (winner != null) {
                closureMessage = String.format(
                        "AUCTION_CLOSED:%s:WINNER:%s:%.2f",
                        auction.getAuctionId(),
                        winner,
                        finalPrice
                );
                System.out.println("[Monitor] Winner: " + winner + " at $" + finalPrice);
            } else {
                closureMessage = String.format("AUCTION_CLOSED:%s:NO_BIDS", auction.getAuctionId());
                System.out.println("[Monitor] Auction ended with no bids.");
            }

            // Notify all clients (Module 4)
            BidBroadcaster.getInstance().broadcast(closureMessage);
        }
    }

}
