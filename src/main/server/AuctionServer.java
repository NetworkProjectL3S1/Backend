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
    private final Map<SocketChannel, main.server.AuctionClientHandler> clients = new HashMap<>();

    // --- Managers ---
    private final main.server.AuctionManager auctionManager;
    private final main.server.BidBroadcaster bidBroadcaster;

    public AuctionServer(int port) throws IOException {
        this.port = port;
        this.selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // --- Initialize managers ---
        this.auctionManager = new main.server.AuctionManager();
        this.auctionManager.setServer(this); // Set server reference for broadcasting
        this.bidBroadcaster = new main.server.BidBroadcaster();
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
                        main.server.AuctionClientHandler handler = (main.server.AuctionClientHandler) key.attachment();
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
        main.server.AuctionClientHandler handler = new main.server.AuctionClientHandler(clientChannel, this);
        clientChannel.register(selector, SelectionKey.OP_READ, handler);
        clients.put(clientChannel, handler);
        System.out.println("New client connected: " + handler.getRemoteAddress());
        handler.write("Welcome to the Auction Server!");
    }

    /**
     * This is the main "router". It just directs messages to the right handler.
     */
    public void processClientMessage(main.server.AuctionClientHandler sender, String message) {
        if (message.startsWith("BID:")) {
            handleBid(sender, message);
        } else if (message.startsWith("WATCH:")) {
            handleWatch(sender, message);
        } else if (message.startsWith("CREATE_AUCTION:")) {
            handleCreateAuction(sender, message);
        } else if (message.startsWith("LIST_AUCTIONS")) {
            handleListAuctions(sender, message);
        } else if (message.startsWith("GET_AUCTION:")) {
            handleGetAuction(sender, message);
        }
        // ... other commands like "LOGIN:", "CHAT:", etc.
    }

    /**
     * Handles a client's request to watch an auction.
     */
    private void handleWatch(main.server.AuctionClientHandler sender, String message) {
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
    private void handleBid(main.server.AuctionClientHandler sender, String message) {
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
            // sender.write("ERROR: You must watch an auction to bid.");
            // return;
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

    /**
     * Handle CREATE_AUCTION command
     * Format:
     * CREATE_AUCTION:itemName:description:sellerId:basePrice:durationMinutes:category
     * Module 2: Auction Creation
     */
    private void handleCreateAuction(main.server.AuctionClientHandler sender, String message) {
        try {
            // Parse the message
            String[] parts = message.split(":", 7); // Limit to 7 parts to preserve description with colons

            if (parts.length < 7) {
                sender.write(
                        "ERROR: Invalid CREATE_AUCTION format. Use CREATE_AUCTION:itemName:description:sellerId:basePrice:durationMinutes:category");
                return;
            }

            String itemName = parts[1];
            String description = parts[2];
            String sellerId = parts[3];
            double basePrice = Double.parseDouble(parts[4]);
            long durationMinutes = Long.parseLong(parts[5]);
            String category = parts[6];

            // Validate parameters
            if (itemName.isEmpty() || sellerId.isEmpty()) {
                sender.write("ERROR: Item name and seller ID cannot be empty");
                return;
            }

            if (basePrice <= 0) {
                sender.write("ERROR: Base price must be greater than 0");
                return;
            }

            if (durationMinutes <= 0) {
                sender.write("ERROR: Duration must be greater than 0");
                return;
            }

            // Create the auction using AuctionManager
            Auction newAuction = auctionManager.createAuction(
                    itemName, description, sellerId, basePrice, durationMinutes, category);

            // Send confirmation to creator
            sender.write("AUCTION_CREATED:" + newAuction.getAuctionId() + ":" + itemName);
            sender.write(newAuction.toDetailString());

            System.out.println("[Server] Auction created: " + newAuction.getAuctionId() +
                    " by " + sellerId);

        } catch (NumberFormatException e) {
            sender.write("ERROR: Invalid number format in CREATE_AUCTION command");
        } catch (Exception e) {
            sender.write("ERROR: Failed to create auction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle LIST_AUCTIONS command
     * Format: LIST_AUCTIONS or LIST_AUCTIONS:category
     */
    private void handleListAuctions(main.server.AuctionClientHandler sender, String message) {
        try {
            String[] parts = message.split(":");

            java.util.Collection<Auction> auctions;

            if (parts.length > 1 && !parts[1].isEmpty()) {
                // List auctions by category
                String category = parts[1];
                auctions = auctionManager.getAuctionsByCategory(category);
                sender.write("AUCTIONS_LIST:category:" + category);
            } else {
                // List all active auctions
                auctions = auctionManager.getActiveAuctions();
                sender.write("AUCTIONS_LIST:all");
            }

            if (auctions.isEmpty()) {
                sender.write("NO_AUCTIONS");
            } else {
                for (Auction auction : auctions) {
                    sender.write(auction.toBroadcastString());
                }
            }

            sender.write("AUCTIONS_LIST_END");

        } catch (Exception e) {
            sender.write("ERROR: Failed to list auctions: " + e.getMessage());
        }
    }

    /**
     * Handle GET_AUCTION command
     * Format: GET_AUCTION:auctionId
     */
    private void handleGetAuction(main.server.AuctionClientHandler sender, String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 2) {
                sender.write("ERROR: Invalid GET_AUCTION format. Use GET_AUCTION:auctionId");
                return;
            }

            String auctionId = parts[1];
            Auction auction = auctionManager.getAuction(auctionId);

            if (auction != null) {
                sender.write(auction.toDetailString());
            } else {
                sender.write("ERROR: Auction not found: " + auctionId);
            }

        } catch (Exception e) {
            sender.write("ERROR: Failed to get auction: " + e.getMessage());
        }
    }

    /**
     * Broadcast a new auction to all connected clients
     * Called by AuctionManager when a new auction is created
     */
    public void broadcastNewAuction(Auction auction) {
        String broadcastMsg = auction.toBroadcastString();

        System.out.println("[Server] Broadcasting new auction: " + auction.getAuctionId());

        for (main.server.AuctionClientHandler client : clients.values()) {
            client.write(broadcastMsg);
        }
    }

    public void clientDisconnected(main.server.AuctionClientHandler handler) {
        clients.remove(handler.getChannel());
        auctionManager.removeWatcherFromAllAuctions(handler);
        System.out.println("Client disconnected: " + handler.getRemoteAddress());
    }

    /**
     * Get the auction manager (for testing or external access)
     */
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }
}