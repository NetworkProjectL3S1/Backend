package main.api.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import main.api.ApiResponse;
import main.model.Auction;
import main.model.Bid;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket Controller for Real-time Bid Updates
 * Handles WebSocket connections and broadcasts bid updates to connected clients
 */
public class WebSocketBidController implements HttpHandler {
    
    private static final Map<String, Set<WebSocketConnection>> auctionSubscribers = new ConcurrentHashMap<>();
    
    public WebSocketBidController() {
        // No dependencies needed - this is a static broadcaster
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        // Handle WebSocket upgrade request
        if ("GET".equals(exchange.getRequestMethod())) {
            String upgradeHeader = exchange.getRequestHeaders().getFirst("Upgrade");
            if ("websocket".equalsIgnoreCase(upgradeHeader)) {
                handleWebSocketUpgrade(exchange);
            } else {
                ApiResponse.sendError(exchange, 400, "WebSocket upgrade required");
            }
        } else {
            ApiResponse.sendError(exchange, 405, "Method not allowed");
        }
    }
    
    private void handleWebSocketUpgrade(HttpExchange exchange) throws IOException {
        // This is a simplified WebSocket handler
        // In production, use a proper WebSocket library like Java-WebSocket
        ApiResponse.sendError(exchange, 501, "WebSocket upgrade not implemented in this simple HTTP server");
    }
    
    /**
     * Broadcast new bid to all clients watching a specific auction
     */
    public static void broadcastBid(String auctionId, Bid bid, Auction auction) {
        Set<WebSocketConnection> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers != null && !subscribers.isEmpty()) {
            String bidUpdate = String.format(
                "{\"type\":\"BID_UPDATE\",\"auctionId\":\"%s\",\"bid\":{\"userId\":\"%s\",\"amount\":%.2f,\"timestamp\":%d},\"currentHighestBid\":%.2f,\"currentHighestBidder\":\"%s\"}",
                auctionId,
                escapeJson(bid.getUserId()),
                bid.getAmount(),
                bid.getTimestamp(),
                auction.getCurrentHighestBid(),
                escapeJson(auction.getCurrentHighestBidder())
            );
            
            System.out.println("[WebSocketBidController] Broadcasting to " + subscribers.size() + " subscribers for auction " + auctionId);
            
            for (WebSocketConnection conn : subscribers) {
                try {
                    conn.send(bidUpdate);
                } catch (Exception e) {
                    System.err.println("[WebSocketBidController] Failed to send to subscriber: " + e.getMessage());
                    subscribers.remove(conn);
                }
            }
        }
    }
    
    /**
     * Broadcast auction expiration notification
     */
    public static void broadcastAuctionExpiration(String auctionId, String notificationJson) {
        Set<WebSocketConnection> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers != null && !subscribers.isEmpty()) {
            System.out.println("[WebSocketBidController] Broadcasting expiration to " + 
                             subscribers.size() + " subscribers for auction " + auctionId);
            
            for (WebSocketConnection conn : subscribers) {
                try {
                    conn.send(notificationJson);
                } catch (Exception e) {
                    System.err.println("[WebSocketBidController] Failed to send expiration notification: " + 
                                     e.getMessage());
                    subscribers.remove(conn);
                }
            }
        }
    }
    
    /**
     * Subscribe a client to auction updates
     */
    public static void subscribe(String auctionId, WebSocketConnection connection) {
        auctionSubscribers.computeIfAbsent(auctionId, k -> new CopyOnWriteArraySet<>()).add(connection);
        System.out.println("[WebSocketBidController] Client subscribed to auction: " + auctionId);
    }
    
    /**
     * Unsubscribe a client from auction updates
     */
    public static void unsubscribe(String auctionId, WebSocketConnection connection) {
        Set<WebSocketConnection> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers != null) {
            subscribers.remove(connection);
            if (subscribers.isEmpty()) {
                auctionSubscribers.remove(auctionId);
            }
        }
        System.out.println("[WebSocketBidController] Client unsubscribed from auction: " + auctionId);
    }
    
    /**
     * Escape JSON special characters
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * WebSocket Connection wrapper
     */
    public static class WebSocketConnection {
        private final Socket socket;
        private final OutputStream out;
        
        public WebSocketConnection(Socket socket, OutputStream out) {
            this.socket = socket;
            this.out = out;
        }
        
        public void send(String message) throws IOException {
            // Send WebSocket frame
            out.write(encodeWebSocketFrame(message));
            out.flush();
        }
        
        private byte[] encodeWebSocketFrame(String message) {
            // Simple text frame encoding
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            int length = messageBytes.length;
            byte[] frame;
            
            if (length < 126) {
                frame = new byte[2 + length];
                frame[0] = (byte) 0x81; // FIN + text frame
                frame[1] = (byte) length;
                System.arraycopy(messageBytes, 0, frame, 2, length);
            } else {
                frame = new byte[4 + length];
                frame[0] = (byte) 0x81;
                frame[1] = 126;
                frame[2] = (byte) ((length >> 8) & 0xFF);
                frame[3] = (byte) (length & 0xFF);
                System.arraycopy(messageBytes, 0, frame, 4, length);
            }
            
            return frame;
        }
        
        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing WebSocket connection: " + e.getMessage());
            }
        }
    }
}
