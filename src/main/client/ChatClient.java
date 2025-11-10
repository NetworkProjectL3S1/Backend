package main.client;

import main.util.WebSocketUtil;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple WebSocket client for testing the chat server
 */
public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    
    private Socket socket;
    private BufferedReader reader;
    private OutputStream outputStream;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private Thread messageListener;
    
    public ChatClient() {}
    
    /**
     * Connect to the chat server
     */
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream = socket.getOutputStream();
            
            System.out.println("Connected to server at " + host + ":" + port);
            
            // Perform WebSocket handshake
            if (performWebSocketHandshake()) {
                isConnected.set(true);
                startMessageListener();
                return true;
            }
            
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
        
        return false;
    }
    
    private boolean performWebSocketHandshake() throws IOException {
        // Generate WebSocket key
        byte[] keyBytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            keyBytes[i] = (byte) (Math.random() * 256);
        }
        String webSocketKey = Base64.getEncoder().encodeToString(keyBytes);
        
        // Send WebSocket handshake request
        String handshakeRequest = 
            "GET /chat HTTP/1.1\r\n" +
            "Host: localhost:" + DEFAULT_PORT + "\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: " + webSocketKey + "\r\n" +
            "Sec-WebSocket-Version: 13\r\n" +
            "\r\n";
        
        outputStream.write(handshakeRequest.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        
        // Read handshake response
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            responseBuilder.append(line).append("\r\n");
        }
        
        String response = responseBuilder.toString();
        
        // Check if handshake was successful
        if (response.contains("101 Switching Protocols")) {
            System.out.println("WebSocket handshake successful!");
            return true;
        } else {
            System.err.println("WebSocket handshake failed!");
            System.err.println("Server response: " + response);
            return false;
        }
    }
    
    private void startMessageListener() {
        messageListener = new Thread(() -> {
            byte[] buffer = new byte[8192];
            
            while (isConnected.get() && !socket.isClosed()) {
                try {
                    int bytesRead = socket.getInputStream().read(buffer);
                    if (bytesRead == -1) {
                        break; // Server disconnected
                    }
                    
                    byte[] frameData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, frameData, 0, bytesRead);
                    
                    handleWebSocketFrame(frameData);
                    
                } catch (IOException e) {
                    if (isConnected.get()) {
                        System.err.println("Error reading from server: " + e.getMessage());
                    }
                    break;
                }
            }
            
            System.out.println("Connection to server lost.");
            isConnected.set(false);
        });
        
        messageListener.setDaemon(true);
        messageListener.start();
    }
    
    private void handleWebSocketFrame(byte[] frameData) {
        if (WebSocketUtil.isCloseFrame(frameData)) {
            System.out.println("Server closed the connection.");
            disconnect();
            return;
        }
        
        String message = WebSocketUtil.decodeTextFrame(frameData);
        if (message != null && !message.trim().isEmpty()) {
            System.out.println(message);
        }
    }
    
    /**
     * Send a message to the server
     */
    public void sendMessage(String message) {
        if (!isConnected.get()) {
            System.err.println("Not connected to server!");
            return;
        }
        
        try {
            byte[] frame = WebSocketUtil.encodeTextFrame(message);
            outputStream.write(frame);
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            disconnect();
        }
    }
    
    /**
     * Disconnect from the server
     */
    public void disconnect() {
        if (!isConnected.get()) {
            return;
        }
        
        isConnected.set(false);
        
        try {
            // Send close frame
            byte[] closeFrame = WebSocketUtil.createCloseFrame();
            outputStream.write(closeFrame);
            outputStream.flush();
            
            // Close streams and socket
            if (reader != null) reader.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            System.out.println("Disconnected from server.");
            
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return isConnected.get();
    }
    
    /**
     * Main method to run the client
     */
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        
        // Parse command line arguments
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                System.exit(1);
            }
        }
        
        ChatClient client = new ChatClient();
        
        System.out.println("Java WebSocket Chat Client");
        System.out.println("=========================");
        System.out.println("Connecting to " + host + ":" + port + "...");
        
        if (client.connect(host, port)) {
            System.out.println("Connected successfully!");
            System.out.println("You can now start chatting. Type '/quit' to exit.");
            System.out.println("Available commands: /help, /users, /time, /bot <message>");
            System.out.println("=========================");
            
            // Start interactive session
            Scanner scanner = new Scanner(System.in);
            
            while (client.isConnected()) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                if (input.equalsIgnoreCase("/quit") || input.equalsIgnoreCase("/exit")) {
                    client.sendMessage("/quit");
                    try {
                        Thread.sleep(500); // Give time for server to process
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
                
                client.sendMessage(input);
            }
            
            client.disconnect();
            scanner.close();
            
        } else {
            System.err.println("Failed to connect to server!");
            System.exit(1);
        }
        
        System.out.println("Client terminated.");
    }
}
