package main.api;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import main.api.controllers.AuctionController;
import main.api.controllers.AuthController;
import main.api.controllers.BidController;
import main.api.controllers.ChatController;
import main.api.controllers.NotificationController;
import main.util.AuctionTimerManager;

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
    private ChatController chatController;
    private NotificationController notificationController;
    
    public ApiServer() throws IOException {
        // Initialize HTTP server
        server = HttpServer.create(new InetSocketAddress(API_PORT), BACKLOG);
        
        // Initialize controllers
        auctionController = new AuctionController();
        bidController = new BidController();
        authController = new AuthController();
        chatController = new ChatController();
        notificationController = new NotificationController();
        
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
        server.createContext("/api/auctions/seller", auctionController);
        server.createContext("/api/auctions", auctionController);
        server.createContext("/api/auctions/create", auctionController);
        server.createContext("/api/auctions/list", auctionController);
        
        // Bid endpoints
        server.createContext("/api/bids", bidController);
        server.createContext("/api/bids/place", bidController);
        server.createContext("/api/bids/history", bidController);
        
        // Chat endpoints
        server.createContext("/api/chat/messages", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                chatController.getMessages(exchange);
            } else {
                ApiResponse.sendError(exchange, 405, "Method not allowed");
            }
        });
        server.createContext("/api/chat/buyers", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                chatController.getBuyers(exchange);
            } else {
                ApiResponse.sendError(exchange, 405, "Method not allowed");
            }
        });
        server.createContext("/api/chat/mark-read", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                chatController.markAsRead(exchange);
            } else {
                ApiResponse.sendError(exchange, 405, "Method not allowed");
            }
        });
        
        // Notification endpoints
        server.createContext("/api/notifications", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                notificationController.getNotifications(exchange);
            } else {
                ApiResponse.sendError(exchange, 405, "Method not allowed");
            }
        });
        server.createContext("/api/notifications/unread-count", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                notificationController.getUnreadCount(exchange);
            } else {
                ApiResponse.sendError(exchange, 405, "Method not allowed");
            }
        });
        server.createContext("/api/notifications/mark-read", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                notificationController.markAsRead(exchange);
            } else {
                ApiResponse.sendError(exchange, 405, "Method not allowed");
            }
        });
        server.createContext("/api/notifications/mark-all-read", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                notificationController.markAllAsRead(exchange);
            } else {
                ApiResponse.sendError(exchange, 405, "Method not allowed");
            }
        });
        
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
        
        // Initialize auction timers for active auctions
        AuctionTimerManager.getInstance().initializeActiveAuctionTimers();
        
        System.out.println("[ApiServer] REST API Server started on http://localhost:" + API_PORT);
        System.out.println("[ApiServer] API endpoints available at http://localhost:" + API_PORT + "/api/");
    }
    
    /**
     * Stop the API server
     */
    public void stop() {
        if (server != null) {
            AuctionTimerManager.getInstance().shutdown();
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
