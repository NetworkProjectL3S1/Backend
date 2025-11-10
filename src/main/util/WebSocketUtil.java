package main.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for WebSocket protocol handling
 */
public class WebSocketUtil {
    
    public static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Pattern WEBSOCKET_KEY_PATTERN = Pattern.compile("Sec-WebSocket-Key: (.+)");
    
    /**
     * Generate WebSocket accept key from client key
     */
    public static String generateAcceptKey(String clientKey) {
        try {
            String concatenated = clientKey + WEBSOCKET_MAGIC_STRING;
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(concatenated.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
    
    /**
     * Extract WebSocket key from HTTP request headers
     */
    public static String extractWebSocketKey(String httpRequest) {
        Matcher matcher = WEBSOCKET_KEY_PATTERN.matcher(httpRequest);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    /**
     * Create WebSocket handshake response
     */
    public static String createHandshakeResponse(String acceptKey) {
        return "HTTP/1.1 101 Switching Protocols\r\n" +
               "Upgrade: websocket\r\n" +
               "Connection: Upgrade\r\n" +
               "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
               "\r\n";
    }
    
    /**
     * Encode a text message as a WebSocket frame
     */
    public static byte[] encodeTextFrame(String message) {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        int messageLength = messageBytes.length;
        
        ByteBuffer buffer;
        
        if (messageLength < 126) {
            buffer = ByteBuffer.allocate(2 + messageLength);
            buffer.put((byte) 0x81); // FIN=1, opcode=1 (text frame)
            buffer.put((byte) messageLength);
        } else if (messageLength < 65536) {
            buffer = ByteBuffer.allocate(4 + messageLength);
            buffer.put((byte) 0x81);
            buffer.put((byte) 126);
            buffer.putShort((short) messageLength);
        } else {
            buffer = ByteBuffer.allocate(10 + messageLength);
            buffer.put((byte) 0x81);
            buffer.put((byte) 127);
            buffer.putLong(messageLength);
        }
        
        buffer.put(messageBytes);
        return buffer.array();
    }
    
    /**
     * Decode a WebSocket frame to extract the message
     */
    public static String decodeTextFrame(byte[] frame) {
        if (frame.length < 2) {
            return null;
        }
        
        int offset = 2;
        byte secondByte = frame[1];
        boolean masked = (secondByte & 0x80) != 0;
        int payloadLength = secondByte & 0x7F;
        
        if (payloadLength == 126) {
            if (frame.length < 4) return null;
            payloadLength = ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
            offset = 4;
        } else if (payloadLength == 127) {
            if (frame.length < 10) return null;
            // For simplicity, we'll assume payload length fits in an int
            payloadLength = (int) (((long)(frame[6] & 0xFF) << 24) |
                                  ((long)(frame[7] & 0xFF) << 16) |
                                  ((long)(frame[8] & 0xFF) << 8) |
                                  ((long)(frame[9] & 0xFF)));
            offset = 10;
        }
        
        byte[] maskingKey = null;
        if (masked) {
            if (frame.length < offset + 4) return null;
            maskingKey = new byte[4];
            System.arraycopy(frame, offset, maskingKey, 0, 4);
            offset += 4;
        }
        
        if (frame.length < offset + payloadLength) {
            return null;
        }
        
        byte[] payload = new byte[payloadLength];
        System.arraycopy(frame, offset, payload, 0, payloadLength);
        
        if (masked && maskingKey != null) {
            for (int i = 0; i < payloadLength; i++) {
                payload[i] ^= maskingKey[i % 4];
            }
        }
        
        return new String(payload, StandardCharsets.UTF_8);
    }
    
    /**
     * Create a close frame
     */
    public static byte[] createCloseFrame() {
        return new byte[]{(byte) 0x88, 0x00}; // FIN=1, opcode=8 (close frame), no payload
    }
    
    /**
     * Create a pong frame (response to ping)
     */
    public static byte[] createPongFrame(byte[] pingData) {
        ByteBuffer buffer = ByteBuffer.allocate(2 + pingData.length);
        buffer.put((byte) 0x8A); // FIN=1, opcode=10 (pong frame)
        buffer.put((byte) pingData.length);
        buffer.put(pingData);
        return buffer.array();
    }
    
    /**
     * Check if the frame is a close frame
     */
    public static boolean isCloseFrame(byte[] frame) {
        return frame.length >= 1 && (frame[0] & 0x0F) == 0x08;
    }
    
    /**
     * Check if the frame is a ping frame
     */
    public static boolean isPingFrame(byte[] frame) {
        return frame.length >= 1 && (frame[0] & 0x0F) == 0x09;
    }
}
