package main.server;

import main.model.Bid;
import main.model.Auction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BidBroadcaster {

    private static BidBroadcaster instance;
    private final ExecutorService broadcastThreadPool;

    private AuctionServer auctionServer;

    // No longer needs a reference to the server
    public BidBroadcaster() {
        this.broadcastThreadPool = Executors.newFixedThreadPool(5);
    }

    public static synchronized BidBroadcaster getInstance() {
        if (instance == null) {
            instance = new BidBroadcaster();
        }
        return instance;
    }

    public void broadcast(String message) {
        if (auctionServer == null) {
            System.err.println("[BROADCASTER] Cannot broadcast: AuctionServer reference is missing.");
            return;
        }

        // Dispatch broadcast to all clients on a separate thread
        broadcastThreadPool.submit(() -> {
            System.out.println("[BROADCASTER] Sending system broadcast: " + message);

            // Use the AuctionServer's helper method to get all active handlers
            for (AuctionClientHandler client : auctionServer.getAllClientHandlers()) {
                if (client.getChannel().isOpen()) {
                    client.write(message);
                }
            }
        });
    }

    // --- ADDED SETTER (FIX for Cannot resolve method 'setAuctionServer') ---
    public void setAuctionServer(AuctionServer server) {
        this.auctionServer = server;
    }

    /**
     * This is YOUR main method. It's now much "smarter".
     * It's called after a bid is successfully validated and placed.
     */
    public void handleNewBid(Auction auction, Bid newBid, AuctionClientHandler sender) {
        // 1. Send immediate confirmation to the bidder
        sender.write("CONFIRM_BID:Your bid of $" + newBid.getAmount() + " is the new highest bid on " + auction.getItemName());

        // 2. Prepare the broadcast message
        String broadcastMessage = newBid.toBroadcastString();

        // 3. Dispatch broadcast to all relevant clients (watchers) on a separate thread
        broadcastThreadPool.submit(() -> {
            System.out.println("[BROADCASTER] Sending bid update for " + auction.getAuctionId());

            // Get the list of clients watching this specific auction
            List<AuctionClientHandler> watchers = auction.getWatchers();

            for (AuctionClientHandler client : watchers) {
                // Ensure the client is still connected and not the original sender (optional confirmation)
                if (client.getChannel().isOpen() && client != sender) {
                    client.write(broadcastMessage);
                }
            }
        });
    }

    /**
     * Broadcasts a new auction to ALL connected clients.
     */
    public void broadcastNewAuction(Auction newAuction) {
        String broadcastMessage = newAuction.toBroadcastString();

        broadcastThreadPool.submit(() -> {
            System.out.println("[BROADCASTER] Sending new auction broadcast: " + newAuction.getAuctionId());

            if (auctionServer != null) {
                // Assuming AuctionServer holds a method to get ALL active client handlers
                for (AuctionClientHandler client : auctionServer.getAllClientHandlers()) {
                    client.write(broadcastMessage);
                }
            }
        });
    }
}