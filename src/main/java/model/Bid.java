package main.java.model;

public class Bid {
    private final String auctionId;
    private final String userId;
    private final double amount;
    private final long timestamp;

    public Bid(String auctionId, String userId, double amount) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    // --- Getters ---
    public String getAuctionId() {
        return auctionId;
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Converts the Bid into a simple string format for broadcasting.
     */
    public String toBroadcastString() {
        // This is a simple serialization format
        return String.format("NEW_BID:%s:%s:%.2f", auctionId, userId, amount);
    }
}