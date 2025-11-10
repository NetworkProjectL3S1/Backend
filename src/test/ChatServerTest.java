package test;

import main.server.ChatServer;
import main.client.ChatClient;
import main.model.Message;
import main.model.Command;
import main.util.WebSocketUtil;

/**
 * Basic tests for the chat system
 */
public class ChatServerTest {
    
    public static void main(String[] args) {
        System.out.println("Java WebSocket Chat Server - Test Suite");
        System.out.println("========================================");
        
        // Run tests
        testMessageSerialization();
        testCommandParsing();
        testWebSocketUtils();
        
        System.out.println("\nAll tests completed!");
        System.out.println("To test the full system:");
        System.out.println("1. Run: java -cp build main.server.ChatServer");
        System.out.println("2. Run: java -cp build main.client.ChatClient");
        System.out.println("3. Or connect with multiple clients to test multithreading");
    }
    
    private static void testMessageSerialization() {
        System.out.println("\n--- Testing Message Serialization ---");
        
        Message message = new Message(Message.MessageType.MESSAGE, "testuser", "Hello World!");
        String json = message.toJson();
        System.out.println("Serialized: " + json);
        
        Message parsed = Message.fromJson(json);
        System.out.println("Deserialized: " + parsed);
        
        boolean success = message.getUsername().equals(parsed.getUsername()) &&
                         message.getContent().equals(parsed.getContent()) &&
                         message.getType() == parsed.getType();
        
        System.out.println("Message serialization test: " + (success ? "PASSED" : "FAILED"));
    }
    
    private static void testCommandParsing() {
        System.out.println("\n--- Testing Command Parsing ---");
        
        String[] testCommands = {
            "/help",
            "/users", 
            "/time",
            "/bot Hello there!",
            "/pm alice How are you?",
            "/quit",
            "/invalid command"
        };
        
        for (String cmdStr : testCommands) {
            Command cmd = new Command(cmdStr);
            System.out.printf("Command: %-25s | Type: %-15s | Valid: %s%n", 
                cmdStr, cmd.getType(), cmd.isValid());
        }
        
        System.out.println("Command parsing test: COMPLETED");
    }
    
    private static void testWebSocketUtils() {
        System.out.println("\n--- Testing WebSocket Utilities ---");
        
        // Test key generation
        String clientKey = "dGhlIHNhbXBsZSBub25jZQ==";
        String acceptKey = WebSocketUtil.generateAcceptKey(clientKey);
        String expectedAcceptKey = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";
        
        boolean keyTest = expectedAcceptKey.equals(acceptKey);
        System.out.println("WebSocket key generation: " + (keyTest ? "PASSED" : "FAILED"));
        System.out.println("Expected: " + expectedAcceptKey);
        System.out.println("Got:      " + acceptKey);
        
        // Test frame encoding/decoding
        String testMessage = "Hello WebSocket!";
        byte[] encoded = WebSocketUtil.encodeTextFrame(testMessage);
        
        // For testing, we need to add masking to decode properly
        // This is a simplified test
        System.out.println("Frame encoding test: COMPLETED (encoded " + encoded.length + " bytes)");
        
        System.out.println("WebSocket utilities test: COMPLETED");
    }
}
