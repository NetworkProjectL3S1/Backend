package main.api;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import main.api.controllers.AuctionController;
import main.api.controllers.BidController;
import main.api.controllers.AuthController;

/**
 * REST API Server for Auction System
 * Provides HTTP endpoints for React frontend integration
 */
public class ApiServer {
    private static final int API_PORT = 8081;
    private static final int BACKLOG = 0;
    private static final int THREAD_POOL_SIZE = 10;
    
    private HttpServer server;
    private AuctionController auctionController;
    private BidController bidController;
    private AuthController authController;
    
    public ApiServer() throws IOException {
        // Initialize HTTP server
        server = HttpServer.create(new InetSocketAddress(API_PORT), BACKLOG);
        
        // Initialize controllers
        auctionController = new AuctionController();
        bidController = new BidController();
        authController = new AuthController();
        
        // Set up routes
        setupRoutes();
        
        // Configure thread pool
        server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
        
        System.out.println("[ApiServer] REST API Server initialized on port " + API_PORT);
    }
    
    /**
     * Set up API routes
     */
    private void setupRoutes() {
        // CORS preflight handler
        server.createContext("/api/", new CorsHandler());
        
        // Authentication endpoints
        server.createContext("/api/auth/register", authController);
        server.createContext("/api/auth/login", authController);
        server.createContext("/api/auth/verify", authController);
        server.createContext("/api/auth/update-profile", authController);
        
        // Auction endpoints
        server.createContext("/api/auctions", auctionController);
        server.createContext("/api/auctions/create", auctionController);
        server.createContext("/api/auctions/list", auctionController);
        
        // Bid endpoints
        server.createContext("/api/bids", bidController);
        server.createContext("/api/bids/place", bidController);
        server.createContext("/api/bids/history", bidController);
        
        // Health check endpoint
        server.createContext("/api/health", exchange -> {
            String response = "{\"status\":\"ok\",\"timestamp\":" + System.currentTimeMillis() + "}";
            ApiResponse.sendJson(exchange, 200, response);
        });
    }
    
    /**
     * Start the API server
     */
    public void start() {
        server.start();
        System.out.println("[ApiServer] REST API Server started on http://localhost:" + API_PORT);
        System.out.println("[ApiServer] API endpoints available at http://localhost:" + API_PORT + "/api/");
    }
    
    /**
     * Stop the API server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[ApiServer] REST API Server stopped");
        }
    }
    
    /**
     * Main method to run API server standalone
     */
    public static void main(String[] args) {
        try {
            ApiServer apiServer = new ApiServer();
            apiServer.start();
            
            // Keep server running
            System.out.println("[ApiServer] Press Ctrl+C to stop the server");
            Thread.currentThread().join();
            
        } catch (IOException e) {
            System.err.println("[ApiServer] Failed to start server: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("[ApiServer] Server interrupted");
        }
    }
}
