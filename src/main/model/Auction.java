package main.model;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import main.server.ClientHandler1;

public class Auction {
    private String auctionId;
    private String itemName;
    private double currentHighestBid;

    private List<ClientHandler1> watchers = new CopyOnWriteArrayList<>();

    public Auction(String auctionId, String itemName, double basePrice) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentHighestBid = basePrice;
    }

    // --- Getters ---
    public String getAuctionId() { return auctionId; }
    public double getCurrentHighestBid() { return currentHighestBid; }

    // --- Bid Logic ---
    public synchronized boolean placeBid(Bid bid) {
        if (bid.getAmount() > this.currentHighestBid) {
            this.currentHighestBid = bid.getAmount();
            return true; // Bid was successful
        }
        return false; // Bid was not high enough
    }

    // --- Watcher Logic ---
    public void addWatcher(ClientHandler1 client) {
        watchers.add(client);
    }

    public void removeWatcher(ClientHandler1 client) {
        watchers.remove(client);
    }

    public List<ClientHandler1> getWatchers() {
        return watchers;
    }
}
