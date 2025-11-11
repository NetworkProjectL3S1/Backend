package main.util;

import main.model.Auction;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-based persistence for auctions using Java I/O
 * Part of Module 2: Auction Creation
 */
public class AuctionFileStorage {
    private static final String AUCTIONS_DIR = "data/auctions/";
    private static final String AUCTIONS_INDEX = "data/auctions/index.dat";
    private static final String BACKUP_DIR = "data/backups/";

    private static AuctionFileStorage instance;

    private AuctionFileStorage() {
        initializeDirectories();
    }

    public static synchronized AuctionFileStorage getInstance() {
        if (instance == null) {
            instance = new AuctionFileStorage();
        }
        return instance;
    }

    /**
     * Initialize storage directories
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(AUCTIONS_DIR));
            Files.createDirectories(Paths.get(BACKUP_DIR));
            System.out.println("[FileStorage] Directories initialized: " + AUCTIONS_DIR);
        } catch (IOException e) {
            System.err.println("[FileStorage] Failed to create directories: " + e.getMessage());
        }
    }

    /**
     * Save a single auction to file
     */
    public synchronized boolean saveAuction(Auction auction) {
        if (auction == null) {
            return false;
        }

        String filename = AUCTIONS_DIR + auction.getAuctionId() + ".dat";

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            oos.writeObject(auction);
            System.out.println("[FileStorage] Auction saved: " + auction.getAuctionId());

            // Update index
            updateIndex(auction.getAuctionId(), true);
            return true;

        } catch (IOException e) {
            System.err.println("[FileStorage] Failed to save auction " +
                    auction.getAuctionId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Load a single auction from file
     */
    public Auction loadAuction(String auctionId) {
        String filename = AUCTIONS_DIR + auctionId + ".dat";

        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            Auction auction = (Auction) ois.readObject();
            System.out.println("[FileStorage] Auction loaded: " + auctionId);
            return auction;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[FileStorage] Failed to load auction " +
                    auctionId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Load all auctions from storage
     */
    public Map<String, Auction> loadAllAuctions() {
        Map<String, Auction> auctions = new ConcurrentHashMap<>();
        Set<String> auctionIds = loadIndex();

        System.out.println("[FileStorage] Loading " + auctionIds.size() + " auctions from storage...");

        for (String auctionId : auctionIds) {
            Auction auction = loadAuction(auctionId);
            if (auction != null) {
                auctions.put(auctionId, auction);
            }
        }

        System.out.println("[FileStorage] Loaded " + auctions.size() + " auctions successfully");
        return auctions;
    }

    /**
     * Save all auctions in bulk
     */
    public synchronized void saveAllAuctions(Map<String, Auction> auctions) {
        System.out.println("[FileStorage] Saving " + auctions.size() + " auctions...");
        int saved = 0;

        for (Auction auction : auctions.values()) {
            if (saveAuction(auction)) {
                saved++;
            }
        }

        System.out.println("[FileStorage] Saved " + saved + " auctions successfully");
    }

    /**
     * Delete an auction file
     */
    public synchronized boolean deleteAuction(String auctionId) {
        String filename = AUCTIONS_DIR + auctionId + ".dat";

        try {
            boolean deleted = Files.deleteIfExists(Paths.get(filename));
            if (deleted) {
                updateIndex(auctionId, false);
                System.out.println("[FileStorage] Auction deleted: " + auctionId);
            }
            return deleted;
        } catch (IOException e) {
            System.err.println("[FileStorage] Failed to delete auction " +
                    auctionId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Update the index file that tracks all auction IDs
     */
    private synchronized void updateIndex(String auctionId, boolean add) {
        Set<String> auctionIds = loadIndex();

        if (add) {
            auctionIds.add(auctionId);
        } else {
            auctionIds.remove(auctionId);
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(AUCTIONS_INDEX))) {
            oos.writeObject(auctionIds);
        } catch (IOException e) {
            System.err.println("[FileStorage] Failed to update index: " + e.getMessage());
        }
    }

    /**
     * Load the index of all auction IDs
     */
    @SuppressWarnings("unchecked")
    private Set<String> loadIndex() {
        File indexFile = new File(AUCTIONS_INDEX);

        if (!indexFile.exists()) {
            return new HashSet<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(AUCTIONS_INDEX))) {
            return (Set<String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[FileStorage] Failed to load index, creating new: " + e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Create a backup of all auctions
     */
    public void createBackup() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String backupFile = BACKUP_DIR + "auctions_backup_" + timestamp + ".dat";

        try {
            Map<String, Auction> allAuctions = loadAllAuctions();

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(backupFile))) {
                oos.writeObject(allAuctions);
                System.out.println("[FileStorage] Backup created: " + backupFile);
            }
        } catch (IOException e) {
            System.err.println("[FileStorage] Failed to create backup: " + e.getMessage());
        }
    }

    /**
     * Export auction to text format for logging/debugging
     */
    public void exportAuctionToText(Auction auction) {
        String filename = AUCTIONS_DIR + auction.getAuctionId() + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("=== AUCTION DETAILS ===");
            writer.println("ID: " + auction.getAuctionId());
            writer.println("Item: " + auction.getItemName());
            writer.println("Description: " + auction.getItemDescription());
            writer.println("Seller: " + auction.getSellerId());
            writer.println("Base Price: $" + auction.getBasePrice());
            writer.println("Current Bid: $" + auction.getCurrentHighestBid());
            writer.println("Highest Bidder: " +
                    (auction.getCurrentHighestBidder() != null ? auction.getCurrentHighestBidder() : "None"));
            writer.println("Category: " + auction.getCategory());
            writer.println("Status: " + auction.getStatus());
            writer.println("Time Remaining: " + auction.getTimeRemaining() / 1000 + " seconds");
            writer.println("Created: " + new java.util.Date(auction.getCreatedTime()));
            writer.println("Ends: " + new java.util.Date(auction.getEndTime()));

            System.out.println("[FileStorage] Text export created: " + filename);
        } catch (IOException e) {
            System.err.println("[FileStorage] Failed to export auction: " + e.getMessage());
        }
    }

    /**
     * Get statistics about stored auctions
     */
    public void printStorageStats() {
        Set<String> auctionIds = loadIndex();
        Map<String, Auction> auctions = loadAllAuctions();

        int active = 0, closed = 0, cancelled = 0;

        for (Auction auction : auctions.values()) {
            switch (auction.getStatus()) {
                case ACTIVE:
                    active++;
                    break;
                case CLOSED:
                    closed++;
                    break;
                case CANCELLED:
                    cancelled++;
                    break;
            }
        }

        System.out.println("\n=== AUCTION STORAGE STATISTICS ===");
        System.out.println("Total Auctions: " + auctionIds.size());
        System.out.println("Active: " + active);
        System.out.println("Closed: " + closed);
        System.out.println("Cancelled: " + cancelled);
        System.out.println("Storage Location: " + new File(AUCTIONS_DIR).getAbsolutePath());
        System.out.println("==================================\n");
    }
}
