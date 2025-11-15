package main.test;

import java.util.List;
import java.util.Map;
import main.model.Auction;
import main.model.Auction.AuctionStatus;
import main.model.Bid;
import main.util.DatabaseManager;

/**
 * Test/Demo class for DatabaseManager
 * Demonstrates basic database operations for auctions and bids
 */
public class DatabaseTest {
    
    public static void main(String[] args) {
        System.out.println("=== DatabaseManager Test ===\n");
        
        DatabaseManager db = DatabaseManager.getInstance();
        
        // Test 1: Create and save auctions
        System.out.println("1. Creating and saving auctions...");
        testSaveAuctions(db);
        
        // Test 2: Load auctions
        System.out.println("\n2. Loading auctions...");
        testLoadAuctions(db);
        
        // Test 3: Create and save bids
        System.out.println("\n3. Creating and saving bids...");
        testSaveBids(db);
        
        // Test 4: Load bids
        System.out.println("\n4. Loading bids...");
        testLoadBids(db);
        
        // Test 5: Update auction status
        System.out.println("\n5. Updating auction status...");
        testUpdateStatus(db);
        
        // Test 6: Get statistics
        System.out.println("\n6. Getting statistics...");
        testStatistics(db);
        
        // Test 7: Query by filters
        System.out.println("\n7. Testing filtered queries...");
        testFilteredQueries(db);
        
        System.out.println("\n=== All Tests Completed ===");
    }
    
    private static void testSaveAuctions(DatabaseManager db) {
        Auction auction1 = new Auction(
            "AUC001",
            "Vintage Watch",
            "Rare 1960s Rolex",
            "seller123",
            1000.0,
            60, // 60 minutes
            "collectibles"
        );
        
        Auction auction2 = new Auction(
            "AUC002",
            "Gaming Laptop",
            "High-end gaming laptop with RTX 4090",
            "seller456",
            2000.0,
            120, // 120 minutes
            "electronics"
        );
        
        Auction auction3 = new Auction(
            "AUC003",
            "Antique Vase",
            "Ming Dynasty vase",
            "seller123",
            5000.0,
            180, // 180 minutes
            "antiques"
        );
        
        boolean success1 = db.saveAuction(auction1);
        boolean success2 = db.saveAuction(auction2);
        boolean success3 = db.saveAuction(auction3);
        
        System.out.println("  Saved auction1: " + success1);
        System.out.println("  Saved auction2: " + success2);
        System.out.println("  Saved auction3: " + success3);
    }
    
    private static void testLoadAuctions(DatabaseManager db) {
        // Load single auction
        Auction auction = db.loadAuction("AUC001");
        if (auction != null) {
            System.out.println("  Loaded: " + auction);
        }
        
        // Load all auctions
        Map<String, Auction> allAuctions = db.loadAllAuctions();
        System.out.println("  Total auctions in database: " + allAuctions.size());
    }
    
    private static void testSaveBids(DatabaseManager db) {
        // Place bids on auction 1
        Bid bid1 = new Bid("AUC001", "buyer001", 1100.0);
        Bid bid2 = new Bid("AUC001", "buyer002", 1200.0);
        Bid bid3 = new Bid("AUC001", "buyer001", 1300.0);
        
        // Place bids on auction 2
        Bid bid4 = new Bid("AUC002", "buyer003", 2100.0);
        Bid bid5 = new Bid("AUC002", "buyer001", 2200.0);
        
        boolean success1 = db.saveBid(bid1);
        boolean success2 = db.saveBid(bid2);
        boolean success3 = db.saveBid(bid3);
        boolean success4 = db.saveBid(bid4);
        boolean success5 = db.saveBid(bid5);
        
        System.out.println("  Saved 5 bids: " + (success1 && success2 && success3 && success4 && success5));
        
        // Update auctions with bids
        Auction auc1 = db.loadAuction("AUC001");
        if (auc1 != null) {
            auc1.placeBid(bid3); // Highest bid
            db.saveAuction(auc1);
        }
        
        Auction auc2 = db.loadAuction("AUC002");
        if (auc2 != null) {
            auc2.placeBid(bid5); // Highest bid
            db.saveAuction(auc2);
        }
    }
    
    private static void testLoadBids(DatabaseManager db) {
        // Load bids for auction
        List<Bid> auc1Bids = db.loadBidsByAuction("AUC001");
        System.out.println("  Bids for AUC001: " + auc1Bids.size());
        for (Bid bid : auc1Bids) {
            System.out.println("    - " + bid.getUserId() + " bid $" + bid.getAmount());
        }
        
        // Load bids by user
        List<Bid> userBids = db.loadBidsByUser("buyer001");
        System.out.println("  Bids by buyer001: " + userBids.size());
        
        // Get bid history
        List<Bid> recentBids = db.getBidHistory("AUC001", 2);
        System.out.println("  Recent 2 bids for AUC001:");
        for (Bid bid : recentBids) {
            System.out.println("    - " + bid.getUserId() + " bid $" + bid.getAmount());
        }
    }
    
    private static void testUpdateStatus(DatabaseManager db) {
        boolean success = db.updateAuctionStatus("AUC003", AuctionStatus.CLOSED);
        System.out.println("  Updated AUC003 to CLOSED: " + success);
        
        Auction auction = db.loadAuction("AUC003");
        if (auction != null) {
            System.out.println("  Verified status: " + auction.getStatus());
        }
    }
    
    private static void testStatistics(DatabaseManager db) {
        int activeCount = db.getAuctionCount(AuctionStatus.ACTIVE);
        int closedCount = db.getAuctionCount(AuctionStatus.CLOSED);
        
        System.out.println("  Active auctions: " + activeCount);
        System.out.println("  Closed auctions: " + closedCount);
        
        int bidCount1 = db.getBidCount("AUC001");
        int bidCount2 = db.getBidCount("AUC002");
        
        System.out.println("  Bids on AUC001: " + bidCount1);
        System.out.println("  Bids on AUC002: " + bidCount2);
    }
    
    private static void testFilteredQueries(DatabaseManager db) {
        // Get active auctions
        List<Auction> activeAuctions = db.loadAuctionsByStatus(AuctionStatus.ACTIVE);
        System.out.println("  Active auctions: " + activeAuctions.size());
        for (Auction auction : activeAuctions) {
            System.out.println("    - " + auction.getAuctionId() + ": " + auction.getItemName());
        }
        
        // Get auctions by seller
        List<Auction> sellerAuctions = db.loadAuctionsBySeller("seller123");
        System.out.println("  Auctions by seller123: " + sellerAuctions.size());
        for (Auction auction : sellerAuctions) {
            System.out.println("    - " + auction.getAuctionId() + ": " + auction.getItemName());
        }
    }
}
