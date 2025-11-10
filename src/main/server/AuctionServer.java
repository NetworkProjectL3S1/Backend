package main.server;

import main.model.Bid;
import main.model.Auction;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AuctionServer {

    private final int port;
    private final Selector selector;
    private final Map<SocketChannel, ClientHandler1> clients = new HashMap<>();

    // --- Managers ---
    private final AuctionManager auctionManager;
    private final BidBroadcaster bidBroadcaster;

    public AuctionServer(int port) throws IOException {
        this.port = port;
        this.selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // --- Initialize managers ---
        this.auctionManager = new AuctionManager();
        this.bidBroadcaster = new BidBroadcaster(); // No longer needs server reference

        // --- FOR TESTING: Create a fake auction on startup ---
        Auction testAuction = new Auction("auction-1", "Test Item", 100.00);
        this.auctionManager.createAuction(testAuction);
    }

    public void start() {
        System.out.println("Server is listening on port " + this.port);
        try {
            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        acceptNewClient(key);
                    } else if (key.isReadable()) {
                        ClientHandler1 handler = (ClientHandler1) key.attachment();
                        if (handler != null) {
                            handler.read();
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void acceptNewClient(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        clientChannel.configureBlocking(false);
        ClientHandler1 handler = new ClientHandler1(clientChannel, this);
        clientChannel.register(selector, SelectionKey.OP_READ, handler);
        clients.put(clientChannel, handler);
        System.out.println("New client connected: " + handler.getRemoteAddress());
        handler.write("Welcome to the Auction Server!");
    }

    /**
     * This is the main "router". It just directs messages to the right handler.
     */
    public void processClientMessage(ClientHandler1 sender, String message) {
        if (message.startsWith("BID:")) {
            handleBid(sender, message);
        } else if (message.startsWith("WATCH:")) {
            // New command: "WATCH:auction-1"
            handleWatch(sender, message);
        }
        // ... other commands like "LOGIN:", "CHAT:", etc.
    }

    /**
     * Handles a client's request to watch an auction.
     */
    private void handleWatch(ClientHandler1 sender, String message) {
        try {
            String auctionId = message.split(":")[1];
            Auction auction = auctionManager.getAuction(auctionId);

            if (auction != null) {
                auction.addWatcher(sender);
                sender.write("OK: You are now watching " + auctionId);
            } else {
                sender.write("ERROR: Auction not found.");
            }
        } catch (Exception e) {
            sender.write("ERROR: Invalid WATCH format. Use WATCH:auctionId");
        }
    }

    /**
     * Handles an incoming bid message (Member 3's logic).
     */
    private void handleBid(ClientHandler1 sender, String message) {
        try {
            String[] parts = message.split(":");
            String auctionId = parts[1];
            double amount = Double.parseDouble(parts[2]);
            String userId = "tempUser"; // TODO: Get from Member 1's login

            Auction auction = auctionManager.getAuction(auctionId);
            if (auction == null) {
                sender.write("ERROR: Auction not found.");
                return;
            }

            // Check if the user is even watching this auction
            // if (!auction.getWatchers().contains(sender)) {
            //     sender.write("ERROR: You must watch an auction to bid.");
            //     return;
            // }

            Bid bid = new Bid(auctionId, userId, amount);

            // --- THIS IS THE KEY INTEGRATION ---
            // 1. Call Member 3's logic (in Auction.java) to place the bid
            boolean success = auction.placeBid(bid);

            if (success) {
                // 2. Call YOUR module (Member 4) to broadcast the valid bid
                this.bidBroadcaster.handleNewBid(auction, bid, sender);
            } else {
                sender.write("ERROR: Bid not high enough. Current bid is " + auction.getCurrentHighestBid());
            }

        } catch (Exception e) {
            sender.write("ERROR: Invalid BID format. Use BID:auctionId:amount");
        }
    }

    // The old "broadcast" method is no longer needed here.
    // The BidBroadcaster is smart enough to do it.

    public void clientDisconnected(ClientHandler1 handler) {
        clients.remove(handler.getChannel());
        // TODO: Should also remove this handler from all auction "watcher" lists
        System.out.println("Client disconnected: " + handler.getRemoteAddress());
    }
}