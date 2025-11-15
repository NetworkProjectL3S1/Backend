package main.api.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import main.api.ApiResponse;
import main.model.Auction;
import main.model.Bid;
import main.util.DatabaseManager;

/**
 * Controller for Bid-related API endpoints
 * Uses thread synchronization to prevent race conditions
 */
public class BidController implements HttpHandler {
    
    private final DatabaseManager dbManager;
    private final ReentrantLock bidLock;
    
    public BidController() {
        this.dbManager = DatabaseManager.getInstance();
        this.bidLock = new ReentrantLock(true); // Fair lock to prevent starvation
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle CORS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (path.endsWith("/place") && "POST".equals(method)) {
                handlePlaceBid(exchange);
            } else if (path.endsWith("/history") && "GET".equals(method)) {
                handleBidHistory(exchange);
            } else if (path.endsWith("/bids") && "POST".equals(method)) {
                handlePlaceBid(exchange);
            } else {
                ApiResponse.sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/bids/place
     * Place a new bid on an auction with thread synchronization
     */
    private void handlePlaceBid(HttpExchange exchange) throws IOException {
        // Read request body
        String requestBody = new BufferedReader(
            new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Parse JSON manually
        Map<String, String> params = parseSimpleJson(requestBody);
        
        String auctionId = params.get("auctionId");
        String userId = params.get("userId");
        String amountStr = params.get("amount");
        
        if (auctionId == null || userId == null || amountStr == null) {
            ApiResponse.sendError(exchange, 400, "Missing required fields: auctionId, userId, amount");
            return;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            
            // Acquire lock to prevent race conditions when multiple bids arrive simultaneously
            bidLock.lock();
            try {
                // Get auction to verify it exists and is active
                Auction auction = dbManager.getAuction(auctionId);
                
                if (auction == null) {
                    ApiResponse.sendError(exchange, 404, "Auction not found");
                    return;
                }
                
                if (auction.getStatus() != Auction.AuctionStatus.ACTIVE) {
                    ApiResponse.sendError(exchange, 400, "Auction is not active");
                    return;
                }
                
                if (auction.hasExpired()) {
                    ApiResponse.sendError(exchange, 400, "Auction has expired");
                    return;
                }
                
                // Re-check current highest bid inside lock to prevent race condition
                if (amount <= auction.getCurrentHighestBid()) {
                    ApiResponse.sendError(exchange, 400, 
                        String.format("Bid amount must be greater than current highest bid (%.2f)", 
                        auction.getCurrentHighestBid()));
                    return;
                }
                
                // Create bid
                Bid bid = new Bid(auctionId, userId, amount);
                
                // Perform database transaction: save bid and update auction atomically
                boolean success = processBidTransaction(bid, auction);
                
                if (success) {
                    // Broadcast bid update to all connected WebSocket clients
                    WebSocketBidController.broadcastBid(auctionId, bid, auction);
                    
                    String json = bidToJson(bid);
                    ApiResponse.sendCreatedRaw(exchange, json);
                } else {
                    ApiResponse.sendError(exchange, 500, "Failed to place bid - transaction rolled back");
                }
                
            } finally {
                // Always release lock, even if exception occurs
                bidLock.unlock();
            }
            
        } catch (NumberFormatException e) {
            ApiResponse.sendError(exchange, 400, "Invalid number format for amount");
        }
    }
    
    /**
     * Process bid transaction atomically
     * Ensures bid is saved and auction is updated in a single database transaction
     */
    private boolean processBidTransaction(Bid bid, Auction auction) {
        try {
            // Use transactional method to ensure atomicity
            // Both bid save and auction update happen in single transaction
            boolean success = dbManager.saveBidWithTransaction(bid, auction);
            
            if (!success) {
                System.err.println("[BidController] Transaction failed - changes rolled back");
                return false;
            }
            
            System.out.println("[BidController] Transaction successful for bid on auction " + 
                             auction.getAuctionId());
            return true;
            
        } catch (Exception e) {
            System.err.println("[BidController] Transaction error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * GET /api/bids/history?auctionId={auctionId}
     * Get bid history for an auction
     */
    private void handleBidHistory(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        
        if (query == null || !query.contains("auctionId=")) {
            ApiResponse.sendError(exchange, 400, "Missing auctionId parameter");
            return;
        }
        
        String auctionId = query.split("auctionId=")[1].split("&")[0];
        
        List<Bid> bids = dbManager.getBidsByAuction(auctionId);
        
        // Convert to JSON array
        StringBuilder jsonArray = new StringBuilder("[");
        boolean first = true;
        for (Bid bid : bids) {
            if (!first) jsonArray.append(",");
            jsonArray.append(bidToJson(bid));
            first = false;
        }
        jsonArray.append("]");
        
        ApiResponse.sendSuccessRaw(exchange, jsonArray.toString());
    }
    
    /**
     * Convert Bid object to JSON string
     */
    private String bidToJson(Bid bid) {
        return String.format(
            "{\"auctionId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f,\"timestamp\":%d}",
            bid.getAuctionId(),
            bid.getUserId(),
            bid.getAmount(),
            bid.getTimestamp()
        );
    }
    
    /**
     * Simple JSON parser for basic key-value pairs
     */
    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> result = new HashMap<>();
        json = json.trim().replaceAll("^\\{|\\}$", "");
        
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String value = kv[1].trim().replaceAll("^\"|\"$", "");
                result.put(key, value);
            }
        }
        
        return result;
    }
}
