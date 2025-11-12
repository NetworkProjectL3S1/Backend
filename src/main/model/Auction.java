package main.model;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- Core Auction Properties ---
    private String auctionId;
    private String itemName;
    private String itemDescription;
    private String sellerId;
    private double basePrice;
    private double currentHighestBid;
    private String currentHighestBidder;

    // --- Timing Properties ---
    private long createdTime;
    private long endTime;
    private long duration; // in milliseconds

    // --- Status Properties ---
    private AuctionStatus status;
    private String category;

    // --- Non-serializable fields ---
    private transient List<main.server.AuctionClientHandler> watchers = new CopyOnWriteArrayList<>();

    public enum AuctionStatus {
        ACTIVE, CLOSED, CANCELLED
    }

    /**
     * Constructor for creating a new auction
     */
    public Auction(String auctionId, String itemName, String itemDescription,
            String sellerId, double basePrice, long durationMinutes, String category) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.sellerId = sellerId;
        this.basePrice = basePrice;
        this.currentHighestBid = basePrice;
        this.currentHighestBidder = null;
        this.createdTime = System.currentTimeMillis();
        this.duration = durationMinutes * 60 * 1000; // convert minutes to milliseconds
        this.endTime = this.createdTime + this.duration;
        this.status = AuctionStatus.ACTIVE;
        this.category = category;
        this.watchers = new CopyOnWriteArrayList<>();
    }

    /**
     * Legacy constructor for backward compatibility
     */
    public Auction(String auctionId, String itemName, double basePrice) {
        this(auctionId, itemName, "No description", "unknown", basePrice, 60, "general");
    }

    // --- Getters ---
    public String getAuctionId() {
        return auctionId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public String getSellerId() {
        return sellerId;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public String getCurrentHighestBidder() {
        return currentHighestBidder;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getDuration() {
        return duration;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public String getCategory() {
        return category;
    }

    /**
     * Get time remaining in milliseconds
     */
    public long getTimeRemaining() {
        if (status != AuctionStatus.ACTIVE) {
            return 0;
        }
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Check if auction has expired
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() >= endTime;
    }

    // --- Setters ---
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public void setCurrentHighestBid(double bid) {
        this.currentHighestBid = bid;
    }

    public void setCurrentHighestBidder(String bidder) {
        this.currentHighestBidder = bidder;
    }

    // --- Bid Logic ---
    public synchronized boolean placeBid(Bid bid) {
        // Check if auction is still active
        if (status != AuctionStatus.ACTIVE) {
            return false;
        }

        // Check if auction has expired
        if (hasExpired()) {
            this.status = AuctionStatus.CLOSED;
            return false;
        }

        if (bid.getAmount() > this.currentHighestBid) {
            this.currentHighestBid = bid.getAmount();
            this.currentHighestBidder = bid.getUserId();
            return true; // Bid was successful
        }
        return false; // Bid was not high enough
    }

    // --- Watcher Logic ---
    public void addWatcher(main.server.AuctionClientHandler client) {
        if (watchers == null) {
            watchers = new CopyOnWriteArrayList<>();
        }
        watchers.add(client);
    }

    public void removeWatcher(main.server.AuctionClientHandler client) {
        if (watchers != null) {
            watchers.remove(client);
        }
    }

    public List<main.server.AuctionClientHandler> getWatchers() {
        if (watchers == null) {
            watchers = new CopyOnWriteArrayList<>();
        }
        return watchers;
    }

    /**
     * Convert auction to broadcast format for sending to clients
     */
    public String toBroadcastString() {
        return String.format("NEW_AUCTION:%s:%s:%s:%.2f:%d:%s:%s",
                auctionId, itemName, itemDescription, basePrice,
                getTimeRemaining() / 1000, // convert to seconds
                category, sellerId);
    }

    /**
     * Convert auction details to string format
     */
    public String toDetailString() {
        return String.format("AUCTION_DETAILS:%s:%s:%s:%.2f:%.2f:%s:%d:%s:%s",
                auctionId, itemName, itemDescription, basePrice, currentHighestBid,
                currentHighestBidder != null ? currentHighestBidder : "none",
                getTimeRemaining() / 1000, category, status);
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + auctionId + '\'' +
                ", item='" + itemName + '\'' +
                ", seller='" + sellerId + '\'' +
                ", basePrice=" + basePrice +
                ", currentBid=" + currentHighestBid +
                ", status=" + status +
                ", timeRemaining=" + getTimeRemaining() / 1000 + "s" +
                '}';
    }
}
