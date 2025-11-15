package main.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import main.model.Message;
import main.util.ThreadPoolManager;

/**
 * Main WebSocket chat server implementation
 */
public class ChatServer {
    private static final int DEFAULT_PORT = 8080;
    private static final String SERVER_NAME = "Java WebSocket Chat Server";
    
    private ServerSocket serverSocket;
    private final int port;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ConcurrentMap<ClientHandler, Boolean> clientHandlers = new ConcurrentHashMap<>();
    
    private final ThreadPoolManager threadPoolManager;
    private final UserManager userManager;
    
    public ChatServer() {
        this(DEFAULT_PORT);
    }
    
    public ChatServer(int port) {
        this.port = port;
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.userManager = UserManager.getInstance();
    }
    
    /**
     * Start the chat server
     */
    public void start() throws IOException {
        if (isRunning.get()) {
            System.out.println("Server is already running!");
            return;
        }
        
        serverSocket = new ServerSocket(port);
        isRunning.set(true);
        
        System.out.println("=================================");
        System.out.println(SERVER_NAME);
        System.out.println("=================================");
        System.out.println("Server started on port: " + port);
        System.out.println("WebSocket endpoint: ws://localhost:" + port + "/chat");
        System.out.println("Server is ready to accept connections...");
        System.out.println("Press Ctrl+C to stop the server");
        System.out.println("=================================");
        
        // Setup shutdown hook
        setupShutdownHook();
        
        // Accept client connections
        acceptConnections();
    }
    
    private void acceptConnections() {
        while (isRunning.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                System.out.println("New client connection from: " + 
                    clientSocket.getRemoteSocketAddress());
                
                // Create and start client handler in thread pool
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clientHandlers.put(clientHandler, true);
                
                threadPoolManager.submitClientHandler(clientHandler);
                
                System.out.println("Active connections: " + clientHandlers.size());
                
            } catch (IOException e) {
                if (isRunning.get()) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Broadcast a message to all connected clients (except sender if specified)
     */
    public void broadcastMessage(Message message, ClientHandler sender) {
        if (message == null) {
            return;
        }
        
        String formattedMessage = formatMessageForBroadcast(message);
        
        // Log the message to server console
        System.out.println("[BROADCAST] 📢 " + formattedMessage);
        System.out.println("[BROADCAST] Total active handlers: " + clientHandlers.size());
        
        // Send to all connected clients
        int sentCount = 0;
        for (ClientHandler handler : clientHandlers.keySet()) {
            if (handler != sender && handler.isConnected()) {
                try {
                    handler.sendMessage(formattedMessage);
                    sentCount++;
                    System.out.println("[BROADCAST] ✅ Sent to client");
                } catch (Exception e) {
                    System.err.println("[BROADCAST] ❌ Error broadcasting to client: " + e.getMessage());
                    // Remove failed handler
                    removeClientHandler(handler);
                }
            }
        }
        System.out.println("[BROADCAST] 📊 Message sent to " + sentCount + " clients");
    }
    
    /**
     * Send a message to a specific user
     */
    public boolean sendMessageToUser(String username, Message message) {
        System.out.println("[ChatServer] 📨 Attempting to send message to user: " + username);
        System.out.println("[ChatServer] Message content: " + message.getContent());
        
        ClientHandler targetHandler = userManager.getClientHandler(username);
        
        if (targetHandler == null) {
            System.out.println("[ChatServer] ❌ No handler found for user: " + username);
            return false;
        }
        
        if (!targetHandler.isConnected()) {
            System.out.println("[ChatServer] ❌ Handler exists but not connected for user: " + username);
            return false;
        }
        
        String formattedMessage = formatMessageForBroadcast(message);
        System.out.println("[ChatServer] ✅ Sending formatted message: " + formattedMessage);
        targetHandler.sendMessage(formattedMessage);
        System.out.println("[ChatServer] ✅ Message sent successfully to " + username);
        return true;
    }
    
    /**
     * Remove a client handler from active connections
     */
    public void removeClientHandler(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        System.out.println("Client handler removed. Active connections: " + clientHandlers.size());
    }
    
    /**
     * Stop the server gracefully
     */
    public void stop() {
        if (!isRunning.get()) {
            return;
        }
        
        System.out.println("\nShutting down server...");
        isRunning.set(false);
        
        // Disconnect all clients
        disconnectAllClients();
        
        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        // Clear user manager
        userManager.clearAllUsers();
        
        // Shutdown thread pools
        threadPoolManager.shutdown();
        
        System.out.println("Server shutdown complete.");
    }
    
    private void disconnectAllClients() {
        System.out.println("Disconnecting all clients...");
        
        clientHandlers.keySet().parallelStream().forEach(handler -> {
            try {
                handler.disconnect();
            } catch (Exception e) {
                System.err.println("Error disconnecting client: " + e.getMessage());
            }
        });
        
        clientHandlers.clear();
    }
    
    private String formatMessageForBroadcast(Message message) {
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss");
        String timestamp = timeFormat.format(new java.util.Date(message.getTimestamp()));
        
        switch (message.getType()) {
            case SYSTEM:
                return "[" + timestamp + "] [SYSTEM] " + message.getContent();
            case BOT_RESPONSE:
                return "[" + timestamp + "] [" + message.getUsername() + "] " + message.getContent();
            case JOIN:
            case LEAVE:
                return "[" + timestamp + "] * " + message.getContent();
            default:
                return "[" + timestamp + "] <" + message.getUsername() + "> " + message.getContent();
        }
    }
    
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nReceived shutdown signal...");
            stop();
        }));
    }
    
    /**
     * Get server statistics
     */
    public String getServerStats() {
        return String.format(
            "Server Statistics:\n" +
            "- Port: %d\n" +
            "- Active connections: %d\n" +
            "- Connected users: %d\n" +
            "- Active client handlers: %d\n" +
            "- Active message processors: %d\n" +
            "- Server running: %s",
            port,
            clientHandlers.size(),
            userManager.getUserCount(),
            threadPoolManager.getActiveClientHandlers(),
            threadPoolManager.getActiveMessageProcessors(),
            isRunning.get()
        );
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    public int getPort() {
        return port;
    }
    
    public int getActiveConnections() {
        return clientHandlers.size();
    }
    
    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.err.println("Port must be between 1024 and 65535");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
        ChatServer server = new ChatServer(port);
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}
