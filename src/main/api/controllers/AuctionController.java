package main.api.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import main.api.ApiResponse;
import main.model.Auction;
import main.util.AuctionTimerManager;
import main.util.DatabaseManager;

/**
 * Controller for Auction-related API endpoints
 */
public class AuctionController implements HttpHandler {
    
    private final DatabaseManager dbManager;
    
    public AuctionController() {
        this.dbManager = DatabaseManager.getInstance();
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
            if (path.endsWith("/create") && "POST".equals(method)) {
                handleCreateAuction(exchange);
            } else if (path.endsWith("/list") && "GET".equals(method)) {
                handleListAuctions(exchange);
            } else if (path.endsWith("/seller") && "GET".equals(method)) {
                handleGetSellerAuctions(exchange);
            } else if (path.matches(".*/auctions/[^/]+/delete") && "DELETE".equals(method)) {
                handleDeleteAuction(exchange);
            } else if (path.matches(".*/auctions/[^/]+") && "GET".equals(method)) {
                handleGetAuction(exchange);
            } else if (path.endsWith("/auctions") && "GET".equals(method)) {
                handleListAuctions(exchange);
            } else if (path.endsWith("/auctions") && "POST".equals(method)) {
                handleCreateAuction(exchange);
            } else {
                ApiResponse.sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/auctions/create
     * Create a new auction using Java NIO
     */
    private void handleCreateAuction(HttpExchange exchange) throws IOException {
        // Read request body using NIO Channel
        String requestBody = readRequestBodyNIO(exchange);
        
        // Parse JSON manually (simple parsing)
        Map<String, String> params = parseSimpleJson(requestBody);
        
        // Validate required fields
        String itemName = params.get("itemName");
        String itemDescription = params.get("itemDescription");
        String sellerId = params.get("sellerId");
        String basePriceStr = params.get("basePrice");
        String durationStr = params.get("duration");
        String category = params.getOrDefault("category", "general");
        
        if (itemName == null || sellerId == null || basePriceStr == null || durationStr == null) {
            ApiResponse.sendError(exchange, 400, "Missing required fields: itemName, sellerId, basePrice, duration");
            return;
        }
        
        try {
            double basePrice = Double.parseDouble(basePriceStr);
            long duration = Long.parseLong(durationStr);
            
            // Create auction
            Auction auction = dbManager.createAuction(
                itemName,
                itemDescription != null ? itemDescription : "",
                sellerId,
                basePrice,
                duration,
                category
            );
            
            if (auction != null) {
                // Schedule expiration timer
                AuctionTimerManager.getInstance().scheduleAuctionExpiration(auction);
                
                String json = auctionToJson(auction);
                ApiResponse.sendCreatedRaw(exchange, json);
            } else {
                ApiResponse.sendError(exchange, 500, "Failed to create auction");
            }
            
        } catch (NumberFormatException e) {
            ApiResponse.sendError(exchange, 400, "Invalid number format for basePrice or duration");
        }
    }
    
    /**
     * Read request body using Java NIO Channel
     */
    private String readRequestBodyNIO(HttpExchange exchange) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(exchange.getRequestBody());
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        StringBuilder requestBody = new StringBuilder();
        
        try {
            while (channel.read(buffer) > 0) {
                buffer.flip();
                requestBody.append(StandardCharsets.UTF_8.decode(buffer));
                buffer.clear();
            }
        } finally {
            channel.close();
        }
        
        return requestBody.toString();
    }
    
    /**
     * GET /api/auctions/list or GET /api/auctions
     * List all auctions
     */
    private void handleListAuctions(HttpExchange exchange) throws IOException {
        Collection<Auction> auctions = dbManager.getAllAuctions();
        
        // Convert to JSON array
        StringBuilder jsonArray = new StringBuilder("[");
        boolean first = true;
        for (Auction auction : auctions) {
            if (!first) jsonArray.append(",");
            jsonArray.append(auctionToJson(auction));
            first = false;
        }
        jsonArray.append("]");
        
        ApiResponse.sendSuccessRaw(exchange, jsonArray.toString());
    }
    
    /**
     * GET /api/auctions/{auctionId}
     * Get specific auction details
     */
    private void handleGetAuction(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String auctionId = path.substring(path.lastIndexOf('/') + 1);
        
        Auction auction = dbManager.getAuction(auctionId);
        
        if (auction != null) {
            String json = auctionToJson(auction);
            ApiResponse.sendSuccessRaw(exchange, json);
        } else {
            ApiResponse.sendError(exchange, 404, "Auction not found");
        }
    }
    
    /**
     * DELETE /api/auctions/{auctionId}/delete
     * Delete an auction
     */
    private void handleDeleteAuction(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String auctionId = path.substring(path.lastIndexOf('/') - 36, path.lastIndexOf('/'));
        
        // Verify auction exists
        Auction auction = dbManager.getAuction(auctionId);
        if (auction == null) {
            ApiResponse.sendError(exchange, 404, "Auction not found");
            return;
        }
        
        boolean deleted = dbManager.deleteAuction(auctionId);
        
        if (deleted) {
            ApiResponse.sendSuccessRaw(exchange, "{\"message\":\"Auction deleted successfully\"}");
        } else {
            ApiResponse.sendError(exchange, 500, "Failed to delete auction");
        }
    }
    
    /**
     * GET /api/auctions/seller?sellerId=xxx
     * Get auctions by seller
     */
    private void handleGetSellerAuctions(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        
        if (query == null || !query.contains("sellerId=")) {
            ApiResponse.sendError(exchange, 400, "Missing sellerId parameter");
            return;
        }
        
        String sellerId = query.split("sellerId=")[1].split("&")[0];
        Collection<Auction> allAuctions = dbManager.getAllAuctions();
        
        // Filter auctions by sellerId
        StringBuilder jsonArray = new StringBuilder("[");
        boolean first = true;
        for (Auction auction : allAuctions) {
            if (auction.getSellerId().equals(sellerId)) {
                if (!first) jsonArray.append(",");
                jsonArray.append(auctionToJson(auction));
                first = false;
            }
        }
        jsonArray.append("]");
        
        ApiResponse.sendSuccessRaw(exchange, jsonArray.toString());
    }
    
    /**
     * Convert Auction object to JSON string
     */
    private String auctionToJson(Auction auction) {
        return String.format(
            "{\"auctionId\":\"%s\",\"itemName\":\"%s\",\"itemDescription\":\"%s\"," +
            "\"sellerId\":\"%s\",\"basePrice\":%.2f,\"currentHighestBid\":%.2f," +
            "\"currentHighestBidder\":%s,\"status\":\"%s\",\"category\":\"%s\"," +
            "\"createdTime\":%d,\"endTime\":%d,\"duration\":%d}",
            auction.getAuctionId(),
            escapeJson(auction.getItemName()),
            escapeJson(auction.getItemDescription()),
            auction.getSellerId(),
            auction.getBasePrice(),
            auction.getCurrentHighestBid(),
            auction.getCurrentHighestBidder() != null ? "\"" + auction.getCurrentHighestBidder() + "\"" : "null",
            auction.getStatus(),
            auction.getCategory(),
            auction.getCreatedTime(),
            auction.getEndTime(),
            auction.getDuration()
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
    
    /**
     * Escape JSON special characters
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
