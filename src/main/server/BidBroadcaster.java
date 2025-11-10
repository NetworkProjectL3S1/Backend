package main.server;

import main.model.Bid;
import main.model.Auction;

public class BidBroadcaster {

    // No longer needs a reference to the server
    public BidBroadcaster() {
        // Constructor is now empty
    }

    /**
     * This is YOUR main method. It's now much "smarter".
     * It's called after a bid is successfully validated and placed.
     */
    public void handleNewBid(Auction auction, Bid bid, AuctionClientHandler sender) {

        // 1. Get the formatted message
        String broadcastMessage = bid.toBroadcastString();
        System.out.println("Broadcasting new bid for " + auction.getAuctionId() + ": " + broadcastMessage);

        // 2. Get the specific list of watchers for THIS auction
        //    (This is the key change!)
        for (AuctionClientHandler watcher : auction.getWatchers()) {

            // Send to everyone in the "room" (except the sender)
            if (watcher != sender) {
                watcher.write(broadcastMessage);
            }
        }

        // 3. Send a private confirmation back to the bidder
        sender.write("CONFIRM: Your bid of " + bid.getAmount() + " was accepted.");
    }
}