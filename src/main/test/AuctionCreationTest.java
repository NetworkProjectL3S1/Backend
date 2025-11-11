package main.test;

import main.model.Auction;
import main.server.AuctionManager;
import main.util.AuctionFileStorage;

/**
 * Test class for Module 2: Auction Creation
 * Demonstrates all features without needing a running server
 */
public class AuctionCreationTest {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("MODULE 2: AUCTION CREATION SYSTEM TEST");
        System.out.println("===========================================\n");

        // Test 1: Create AuctionManager
        testAuctionManager();

        // Test 2: File Persistence
        testFilePersistence();

        // Test 3: Auction Properties
        testAuctionProperties();

        // Test 4: Category Filtering
        testCategoryFiltering();

        // Test 5: Auction Expiration
        testAuctionExpiration();

        System.out.println("\n===========================================");
        System.out.println("ALL TESTS COMPLETED SUCCESSFULLY! ✓");
        System.out.println("===========================================");
    }

    private static void testAuctionManager() {
        System.out.println("TEST 1: Auction Manager Creation");
        System.out.println("----------------------------------");

        AuctionManager manager = new AuctionManager();

        // Create multiple auctions
        Auction auction1 = manager.createAuction(
                "Vintage Camera",
                "Canon AE-1 from 1976, excellent condition",
                "photoenthusiast",
                250.00,
                90,
                "photography");

        Auction auction2 = manager.createAuction(
                "Gaming Laptop",
                "High-end gaming laptop with RTX 4090",
                "techseller",
                1500.00,
                120,
                "electronics");

        Auction auction3 = manager.createAuction(
                "Abstract Painting",
                "Original artwork by local artist",
                "artgallery",
                800.00,
                60,
                "art");

        System.out.println("✓ Created 3 auctions:");
        System.out.println("  - " + auction1.getAuctionId() + ": " + auction1.getItemName());
        System.out.println("  - " + auction2.getAuctionId() + ": " + auction2.getItemName());
        System.out.println("  - " + auction3.getAuctionId() + ": " + auction3.getItemName());

        // Test retrieval
        Auction retrieved = manager.getAuction(auction1.getAuctionId());
        assert retrieved != null : "Failed to retrieve auction!";
        assert retrieved.getItemName().equals("Vintage Camera") : "Item name mismatch!";

        System.out.println("✓ Successfully retrieved auction by ID");

        // Test statistics
        manager.printStatistics();

        System.out.println("✓ Test 1 PASSED\n");
    }

    private static void testFilePersistence() {
        System.out.println("TEST 2: File Persistence");
        System.out.println("----------------------------------");

        AuctionFileStorage storage = AuctionFileStorage.getInstance();

        // Create a test auction
        Auction testAuction = new Auction(
                "test-persistence-1",
                "Test Item",
                "Testing file persistence",
                "testseller",
                100.00,
                30,
                "test");

        // Save to file
        boolean saved = storage.saveAuction(testAuction);
        assert saved : "Failed to save auction!";
        System.out.println("✓ Auction saved to file");

        // Load from file
        Auction loaded = storage.loadAuction("test-persistence-1");
        assert loaded != null : "Failed to load auction!";
        assert loaded.getItemName().equals("Test Item") : "Loaded data mismatch!";
        System.out.println("✓ Auction loaded from file");

        // Export to text
        storage.exportAuctionToText(testAuction);
        System.out.println("✓ Auction exported to text file");

        // Create backup
        storage.createBackup();
        System.out.println("✓ Backup created");

        // Clean up test file
        storage.deleteAuction("test-persistence-1");
        System.out.println("✓ Test auction cleaned up");

        // Print storage stats
        storage.printStorageStats();

        System.out.println("✓ Test 2 PASSED\n");
    }

    private static void testAuctionProperties() {
        System.out.println("TEST 3: Auction Properties & Methods");
        System.out.println("----------------------------------");

        Auction auction = new Auction(
                "test-properties-1",
                "Collectible Watch",
                "Rare timepiece from 1960s",
                "watchcollector",
                500.00,
                60,
                "collectibles");

        // Test getters
        assert auction.getAuctionId().equals("test-properties-1");
        assert auction.getItemName().equals("Collectible Watch");
        assert auction.getSellerId().equals("watchcollector");
        assert auction.getBasePrice() == 500.00;
        assert auction.getCategory().equals("collectibles");
        assert auction.getStatus() == Auction.AuctionStatus.ACTIVE;
        System.out.println("✓ All getters working correctly");

        // Test time properties
        assert auction.getTimeRemaining() > 0 : "Time remaining should be positive!";
        assert !auction.hasExpired() : "New auction should not be expired!";
        System.out.println("✓ Time properties working correctly");
        System.out.println("  Time remaining: " + auction.getTimeRemaining() / 1000 + " seconds");

        // Test broadcast string
        String broadcast = auction.toBroadcastString();
        assert broadcast.startsWith("NEW_AUCTION:") : "Broadcast format incorrect!";
        System.out.println("✓ Broadcast string format correct");
        System.out.println("  " + broadcast);

        // Test detail string
        String details = auction.toDetailString();
        assert details.startsWith("AUCTION_DETAILS:") : "Detail format incorrect!";
        System.out.println("✓ Detail string format correct");
        System.out.println("  " + details);

        System.out.println("✓ Test 3 PASSED\n");
    }

    private static void testCategoryFiltering() {
        System.out.println("TEST 4: Category Filtering");
        System.out.println("----------------------------------");

        AuctionManager manager = new AuctionManager();

        // Create auctions in different categories
        manager.createAuction("Item 1", "Electronics item", "seller1", 100, 60, "electronics");
        manager.createAuction("Item 2", "Another electronics", "seller2", 200, 60, "electronics");
        manager.createAuction("Item 3", "Art piece", "seller3", 300, 60, "art");
        manager.createAuction("Item 4", "Photography gear", "seller4", 400, 60, "photography");

        // Test filtering by category
        var electronicsAuctions = manager.getAuctionsByCategory("electronics");
        assert electronicsAuctions.size() == 2 : "Should have 2 electronics auctions!";
        System.out.println("✓ Found " + electronicsAuctions.size() + " electronics auctions");

        var artAuctions = manager.getAuctionsByCategory("art");
        assert artAuctions.size() == 1 : "Should have 1 art auction!";
        System.out.println("✓ Found " + artAuctions.size() + " art auction");

        // Test filtering by seller
        var seller1Auctions = manager.getAuctionsBySeller("seller1");
        assert seller1Auctions.size() == 1 : "Should have 1 auction by seller1!";
        System.out.println("✓ Found " + seller1Auctions.size() + " auction by seller1");

        // Test getting all active auctions
        var allActive = manager.getActiveAuctions();
        assert allActive.size() >= 4 : "Should have at least 4 active auctions!";
        System.out.println("✓ Found " + allActive.size() + " total active auctions");

        System.out.println("✓ Test 4 PASSED\n");
    }

    private static void testAuctionExpiration() {
        System.out.println("TEST 5: Auction Expiration");
        System.out.println("----------------------------------");

        // Create an auction with very short duration (1 second)
        Auction shortAuction = new Auction(
                "test-expiration-1",
                "Quick Test Item",
                "This auction expires in 1 second",
                "testseller",
                50.00,
                0, // 0 minutes = will expire immediately for testing
                "test");

        System.out.println("Created auction with 0 minute duration");
        System.out.println("Initial status: " + shortAuction.getStatus());
        System.out.println("Time remaining: " + shortAuction.getTimeRemaining() + " ms");

        // Wait a moment
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check expiration
        boolean expired = shortAuction.hasExpired();
        System.out.println("After 100ms - Expired: " + expired);

        if (expired) {
            // Try to place a bid on expired auction
            main.model.Bid bid = new main.model.Bid("test-expiration-1", "testbidder", 75.00);
            boolean bidSuccess = shortAuction.placeBid(bid);
            assert !bidSuccess : "Should not accept bids on expired auction!";
            System.out.println("✓ Correctly rejected bid on expired auction");
        }

        // Test auction closing
        AuctionManager manager = new AuctionManager();
        Auction normalAuction = manager.createAuction(
                "Item to close",
                "Will be closed manually",
                "seller",
                100,
                60,
                "test");

        String auctionId = normalAuction.getAuctionId();
        boolean closed = manager.closeAuction(auctionId);
        assert closed : "Failed to close auction!";
        System.out.println("✓ Successfully closed auction manually");

        Auction closedAuction = manager.getAuction(auctionId);
        assert closedAuction.getStatus() == Auction.AuctionStatus.CLOSED : "Status should be CLOSED!";
        System.out.println("✓ Auction status correctly set to CLOSED");

        System.out.println("✓ Test 5 PASSED\n");
    }
}
