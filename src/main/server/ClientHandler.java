package main.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import main.model.ChatMessage;
import main.model.Command;
import main.model.Message;
import main.model.User;
import main.util.DatabaseManager;
import main.util.ThreadPoolManager;
import main.util.WebSocketUtil;

/**
 * Handles individual client connections using WebSocket protocol
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ChatServer server;
    private final UserManager userManager;
    private final ChatBot chatBot;
    
    private BufferedReader reader;
    private OutputStream outputStream;
    private User user;
    private boolean isConnected = false;
    private boolean webSocketHandshakeComplete = false;
    
    public ClientHandler(Socket clientSocket, ChatServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.userManager = UserManager.getInstance();
        this.chatBot = new ChatBot();
    }
    
    @Override
    public void run() {
        try {
            setupStreams();
            handleConnection();
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void setupStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outputStream = clientSocket.getOutputStream();
        isConnected = true;
    }
    
    private void handleConnection() throws IOException {
        // First, handle WebSocket handshake
        if (!performWebSocketHandshake()) {
            return;
        }
        
        // Request username
        requestUsername();
        
        // Start listening for messages
        byte[] buffer = new byte[8192];
        while (isConnected && !clientSocket.isClosed()) {
            try {
                int bytesRead = clientSocket.getInputStream().read(buffer);
                if (bytesRead == -1) {
                    break; // Client disconnected
                }
                
                byte[] frameData = new byte[bytesRead];
                System.arraycopy(buffer, 0, frameData, 0, bytesRead);
                
                handleWebSocketFrame(frameData);
                
            } catch (IOException e) {
                if (isConnected) {
                    System.err.println("Error reading from client: " + e.getMessage());
                }
                break;
            }
        }
    }
    
    private boolean performWebSocketHandshake() throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        String line;
        
        // Read HTTP request headers
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            requestBuilder.append(line).append("\r\n");
        }
        
        String httpRequest = requestBuilder.toString();
        System.out.println("WebSocket handshake request received");
        
        // Extract WebSocket key
        String webSocketKey = WebSocketUtil.extractWebSocketKey(httpRequest);
        if (webSocketKey == null) {
            System.err.println("Invalid WebSocket handshake - missing Sec-WebSocket-Key");
            return false;
        }
        
        // Generate accept key and send response
        String acceptKey = WebSocketUtil.generateAcceptKey(webSocketKey);
        String response = WebSocketUtil.createHandshakeResponse(acceptKey);
        
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        
        webSocketHandshakeComplete = true;
        System.out.println("WebSocket handshake completed successfully");
        return true;
    }
    
    private void requestUsername() {
        sendMessage("Please enter your username:");
    }
    
    private void handleWebSocketFrame(byte[] frameData) {
        if (WebSocketUtil.isCloseFrame(frameData)) {
            handleClientDisconnect();
            return;
        }
        
        if (WebSocketUtil.isPingFrame(frameData)) {
            // Respond with pong
            byte[] pongFrame = WebSocketUtil.createPongFrame(new byte[0]);
            try {
                outputStream.write(pongFrame);
                outputStream.flush();
            } catch (IOException e) {
                System.err.println("Error sending pong: " + e.getMessage());
            }
            return;
        }
        
        String message = WebSocketUtil.decodeTextFrame(frameData);
        if (message != null && !message.trim().isEmpty()) {
            handleMessage(message.trim());
        }
    }
    
    private void handleMessage(String messageContent) {
        if (user == null) {
            // This is the username
            handleUsernameRegistration(messageContent);
            return;
        }
        
        // Process the message
        ThreadPoolManager.getInstance().submitMessageProcessor(() -> {
            processMessage(messageContent);
        });
    }
    
    private void handleUsernameRegistration(String username) {
        username = username.trim();
        
        if (username.isEmpty() || username.length() > 20) {
            sendMessage("Username must be between 1 and 20 characters. Please try again:");
            return;
        }
        
        if (!userManager.isUsernameAvailable(username)) {
            sendMessage("Username '" + username + "' is already taken. Please choose another:");
            return;
        }
        
        // Create user and register
        user = new User(username, generateSessionId());
        if (userManager.addUser(user, this)) {
            sendMessage("Welcome, " + username + "! You are now connected to the chat.");
            
            // Send welcome message to user
            Message welcomeMessage = chatBot.generateWelcomeMessage(username);
            sendMessage(formatMessageForDisplay(welcomeMessage));
            
            // Broadcast join notification to other users
            Message joinMessage = new Message(Message.MessageType.JOIN, username, username + " has joined the chat!");
            server.broadcastMessage(joinMessage, this);
            
            System.out.println("User registered: " + username);
        } else {
            sendMessage("Failed to register username. Please try again:");
        }
    }
    
    private void processMessage(String messageContent) {
        if (messageContent.startsWith("/")) {
            // Handle command
            Command command = new Command(messageContent);
            
            if (command.getType() == Command.CommandType.QUIT) {
                handleClientDisconnect();
                return;
            }
            
            if (command.getType() == Command.CommandType.PRIVATE_MESSAGE) {
                handlePrivateMessage(command);
                return;
            }
            
            Message response = chatBot.processCommand(command, user.getUsername());
            sendMessage(formatMessageForDisplay(response));
            
        } else {
            // Regular chat message
            Message message = new Message(Message.MessageType.MESSAGE, user.getUsername(), messageContent);
            
            // Broadcast to all users
            server.broadcastMessage(message, null);
            
            // Generate bot response occasionally (10% chance)
            if (Math.random() < 0.1) {
                Message botResponse = new Message(
                    Message.MessageType.BOT_RESPONSE,
                    "ChatBot",
                    chatBot.generateBotResponse(messageContent, user.getUsername())
                );
                
                // Send bot response after a short delay
                ThreadPoolManager.getInstance().submitMessageProcessor(() -> {
                    try {
                        Thread.sleep(1000 + (long)(Math.random() * 2000)); // 1-3 seconds delay
                        server.broadcastMessage(botResponse, null);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
    }
    
    private void handlePrivateMessage(Command command) {
        Message privateMessage = chatBot.processCommand(command, user.getUsername());
        
        System.out.println("[PrivateMsg] Processing private message from: " + user.getUsername());
        System.out.println("[PrivateMsg] Target user: " + privateMessage.getTargetUser());
        System.out.println("[PrivateMsg] Message content: " + privateMessage.getContent());
        
        if (privateMessage.getTargetUser() != null) {
            // Send to target user
            ClientHandler targetHandler = userManager.getClientHandler(privateMessage.getTargetUser());
            
            if (targetHandler != null) {
                System.out.println("[PrivateMsg] ✅ Target handler found for: " + privateMessage.getTargetUser());
                
                // Extract message content without [Private] prefix but keep auction tags
                String content = privateMessage.getContent();
                if (content.startsWith("[Private] ")) {
                    content = content.substring(10); // Remove "[Private] " (10 characters)
                }
                
                System.out.println("[PrivateMsg] From: " + user.getUsername() + " To: " + 
                    privateMessage.getTargetUser() + " Content: " + content);
                
                // Extract auction ID if present and save to database
                String auctionId = extractAuctionId(content);
                if (auctionId != null) {
                    System.out.println("[PrivateMsg] Auction ID extracted: " + auctionId);
                    
                    // Remove [Auction:ID] tag from content for storage
                    String cleanContent = content.replaceFirst("\\[Auction:" + auctionId + "\\]\\s*", "");
                    
                    ChatMessage chatMessage = new ChatMessage(
                        auctionId,
                        user.getUsername(),
                        privateMessage.getTargetUser(),
                        cleanContent,
                        System.currentTimeMillis()
                    );
                    
                    DatabaseManager.getInstance().saveChatMessage(chatMessage);
                    System.out.println("[PrivateMsg] ✅ Saved to database: auction=" + auctionId + ", from=" + 
                        user.getUsername() + ", to=" + privateMessage.getTargetUser());
                } else {
                    System.out.println("[PrivateMsg] ⚠️ No auction ID found in message");
                }
                
                // Send to recipient
                String toRecipient = "[Private from " + user.getUsername() + "] " + content;
                System.out.println("[PrivateMsg] 📤 Sending to recipient: " + toRecipient);
                targetHandler.sendMessage(toRecipient);
                
                // Confirm to sender (echo)
                String toSender = "[Private to " + privateMessage.getTargetUser() + "] " + content;
                System.out.println("[PrivateMsg] 📤 Sending echo to sender: " + toSender);
                sendMessage(toSender);
                
                System.out.println("[PrivateMsg] ✅ Message delivery complete");
            } else {
                System.out.println("[PrivateMsg] ❌ Target handler NOT found for: " + privateMessage.getTargetUser());
                System.out.println("[PrivateMsg] Available users: " + userManager.getAllUsers());
                sendMessage("[Error] User '" + privateMessage.getTargetUser() + "' is not online");
            }
        } else {
            System.out.println("[PrivateMsg] ❌ No target user specified");
            // Error message from bot
            sendMessage(formatMessageForDisplay(privateMessage));
        }
    }
    
    /**
     * Extract auction ID from message content
     */
    private String extractAuctionId(String content) {
        if (content == null) return null;
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[Auction:([^\\]]+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    public void sendMessage(String message) {
        if (!isConnected || !webSocketHandshakeComplete) {
            System.out.println("[ClientHandler] ❌ Cannot send message - not connected or handshake not complete");
            return;
        }
        
        try {
            System.out.println("[ClientHandler] 📨 Sending to " + 
                (user != null ? user.getUsername() : "unknown") + ": " + message);
            byte[] frame = WebSocketUtil.encodeTextFrame(message);
            outputStream.write(frame);
            outputStream.flush();
            System.out.println("[ClientHandler] ✅ Message sent successfully to " + 
                (user != null ? user.getUsername() : "unknown"));
        } catch (IOException e) {
            System.err.println("[ClientHandler] ❌ Error sending message to " + 
                (user != null ? user.getUsername() : "unknown user") + ": " + e.getMessage());
            handleClientDisconnect();
        }
    }
    
    private void handleClientDisconnect() {
        if (user != null) {
            Message goodbyeMessage = chatBot.generateGoodbyeMessage(user.getUsername());
            server.broadcastMessage(goodbyeMessage, this);
            userManager.removeUser(user.getUsername());
        }
        
        disconnect();
    }
    
    public void disconnect() {
        isConnected = false;
        try {
            if (webSocketHandshakeComplete) {
                byte[] closeFrame = WebSocketUtil.createCloseFrame();
                outputStream.write(closeFrame);
                outputStream.flush();
            }
        } catch (IOException e) {
            // Ignore errors during disconnect
        }
    }
    
    private void cleanup() {
        try {
            if (reader != null) reader.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
        
        server.removeClientHandler(this);
        System.out.println("Client connection closed and cleaned up");
    }
    
    private String formatMessageForDisplay(Message message) {
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
    
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    public User getUser() {
        return user;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
}
