package main.util;

import java.util.concurrent.*;
import java.util.Map;
import main.model.Auction;
import main.api.controllers.WebSocketBidController;

/**
 * Manages auction timers and handles auction expiration
 * Sends real-time notifications to winners and sellers
 */
public class AuctionTimerManager {
    private static AuctionTimerManager instance;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> auctionTimers;
    private final DatabaseManager dbManager;
    
    private AuctionTimerManager() {
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.auctionTimers = new ConcurrentHashMap<>();
        this.dbManager = DatabaseManager.getInstance();
    }
    
    public static synchronized AuctionTimerManager getInstance() {
        if (instance == null) {
            instance = new AuctionTimerManager();
        }
        return instance;
    }
    
    /**
     * Schedule auction expiration timer
     */
    public void scheduleAuctionExpiration(Auction auction) {
        if (auction == null || auction.hasExpired()) {
            return;
        }
        
        String auctionId = auction.getAuctionId();
        
        // Cancel existing timer if any
        cancelTimer(auctionId);
        
        // Calculate delay until auction ends
        long currentTime = System.currentTimeMillis();
        long endTime = auction.getEndTime();
        long delay = endTime - currentTime;
        
        if (delay <= 0) {
            // Already expired, process immediately
            handleAuctionExpiration(auction);
            return;
        }
        
        // Schedule expiration task
        ScheduledFuture<?> future = scheduler.schedule(
            () -> handleAuctionExpiration(auction),
            delay,
            TimeUnit.MILLISECONDS
        );
        
        auctionTimers.put(auctionId, future);
        
        System.out.println("[AuctionTimerManager] Timer scheduled for auction " + auctionId + 
                         " (expires in " + (delay / 1000) + " seconds)");
    }
    
    /**
     * Handle auction expiration
     */
    private void handleAuctionExpiration(Auction auction) {
        String auctionId = auction.getAuctionId();
        
        System.out.println("[AuctionTimerManager] Auction expired: " + auctionId);
        
        // Update auction status to CLOSED
        auction.setStatus(Auction.AuctionStatus.CLOSED);
        dbManager.updateAuctionStatus(auctionId, Auction.AuctionStatus.CLOSED);
        
        // Get winner and seller info
        String winner = auction.getCurrentHighestBidder();
        String seller = auction.getSellerId();
        double finalPrice = auction.getCurrentHighestBid();
        String itemName = auction.getItemName();
        
        // Broadcast expiration notification to all subscribers
        broadcastExpiration(auctionId, auction, winner, seller, finalPrice, itemName);
        
        // Remove timer
        auctionTimers.remove(auctionId);
    }
    
    /**
     * Broadcast auction expiration notification via WebSocket
     */
    private void broadcastExpiration(String auctionId, Auction auction, String winner, 
                                     String seller, double finalPrice, String itemName) {
        // Create expiration notification JSON
        String notification = String.format(
            "{\"type\":\"AUCTION_EXPIRED\",\"auctionId\":\"%s\",\"itemName\":\"%s\"," +
            "\"winner\":%s,\"seller\":\"%s\",\"finalPrice\":%.2f,\"status\":\"CLOSED\"}",
            auctionId,
            escapeJson(itemName),
            winner != null ? "\"" + escapeJson(winner) + "\"" : "null",
            escapeJson(seller),
            finalPrice
        );
        
        // Broadcast to all auction subscribers
        WebSocketBidController.broadcastAuctionExpiration(auctionId, notification);
        
        // Save notifications to database
        if (winner != null) {
            main.model.Notification winnerNotif = new main.model.Notification(
                winner,
                "BID_WON",
                "Congratulations! You won an auction",
                "You won the auction for \"" + itemName + "\" with a bid of $" + finalPrice,
                auctionId
            );
            dbManager.saveNotification(winnerNotif);
            System.out.println("[AuctionTimerManager] Winner notification sent to: " + winner);
        }
        
        main.model.Notification sellerNotif = new main.model.Notification(
            seller,
            "AUCTION_EXPIRED",
            "Auction Expired",
            "Your auction for \"" + itemName + "\" has ended" + 
            (winner != null ? " with a winning bid of $" + finalPrice : " with no bids"),
            auctionId
        );
        dbManager.saveNotification(sellerNotif);
        System.out.println("[AuctionTimerManager] Seller notification sent to: " + seller);
    }
    
    /**
     * Cancel auction timer
     */
    public void cancelTimer(String auctionId) {
        ScheduledFuture<?> future = auctionTimers.remove(auctionId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            System.out.println("[AuctionTimerManager] Timer cancelled for auction: " + auctionId);
        }
    }
    
    /**
     * Initialize timers for all active auctions on startup
     */
    public void initializeActiveAuctionTimers() {
        System.out.println("[AuctionTimerManager] Initializing timers for active auctions...");
        
        for (Auction auction : dbManager.getAllAuctions()) {
            if (auction.getStatus() == Auction.AuctionStatus.ACTIVE) {
                if (auction.hasExpired()) {
                    // Already expired, update status immediately
                    handleAuctionExpiration(auction);
                } else {
                    // Schedule timer
                    scheduleAuctionExpiration(auction);
                }
            }
        }
        
        System.out.println("[AuctionTimerManager] Active timers: " + auctionTimers.size());
    }
    
    /**
     * Get remaining time for auction in milliseconds
     */
    public long getRemainingTime(String auctionId) {
        Auction auction = dbManager.getAuction(auctionId);
        if (auction == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long endTime = auction.getEndTime();
        long remaining = endTime - currentTime;
        
        return Math.max(0, remaining);
    }
    
    /**
     * Escape JSON special characters
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Shutdown scheduler
     */
    public void shutdown() {
        System.out.println("[AuctionTimerManager] Shutting down timer manager...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
