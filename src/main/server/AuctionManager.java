package main.server;

import main.model.Auction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

    // A thread-safe map to hold all active auctions
    // Key: auctionId (String), Value: Auction object
    private Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();

    // This method will be called by Member 2's module
    public void createAuction(Auction auction) {
        activeAuctions.put(auction.getAuctionId(), auction);
        System.out.println("New Auction created: " + auction.getAuctionId());
    }

    public Auction getAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    /**
     * Removes a specific ClientHandler from the watcher list of ALL auctions.
     * This is used for cleanup when a client disconnects.
     */
    public void removeWatcherFromAllAuctions(ClientHandler1 handler) {
        for (Auction auction : activeAuctions.values()) {
            auction.removeWatcher(handler);
        }
    }
}
