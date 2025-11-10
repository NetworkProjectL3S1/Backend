package main.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientHandler1 {

    private final SocketChannel channel;
    private final AuctionServer server;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    // We'll use a simple line-based protocol (messages end with \n)
    private final StringBuilder messageBuffer = new StringBuilder();

    public ClientHandler1(SocketChannel channel, AuctionServer server) {
        this.channel = channel;
        this.server = server;
    }

    /**
     * Reads data from the client's channel.
     * This is called by AuctionServer when the selector says this channel is "readable".
     */
    public void read() {
        try {
            int bytesRead = channel.read(readBuffer);

            if (bytesRead == -1) {
                // Client disconnected
                server.clientDisconnected(this);
                channel.close();
                return;
            }

            if (bytesRead > 0) {
                // Flip the buffer to read data out
                readBuffer.flip();

                // Decode bytes to characters
                while (readBuffer.hasRemaining()) {
                    char c = (char) readBuffer.get();
                    if (c == '\n') {
                        // End of a message
                        processMessage(messageBuffer.toString());
                        messageBuffer.setLength(0); // Clear buffer for next message
                    } else if (c != '\r') {
                        // Append char (ignore carriage return)
                        messageBuffer.append(c);
                    }
                }

                // Compact the buffer to save any partial message
                readBuffer.compact();
            }
        } catch (IOException e) {
            // IO Error, treat as disconnect
            try {
                server.clientDisconnected(this);
                channel.close();
            } catch (IOException ioException) {
                // Ignore
            }
        }
    }

    /**
     * Processes a complete message received from the client.
     */
    private void processMessage(String message) {
        System.out.println("Message from " + getRemoteAddress() + ": " + message);

        // Pass the message to the main server for routing
        // The server will decide what to do (e.g., call Bidding System)
        server.processClientMessage(this, message);
    }

    /**
     * Writes a message TO this client.
     * This is called by your BidBroadcaster.
     */
    public void write(String message) {
        try {
            // Ensure message ends with a newline for our protocol
            if (!message.endsWith("\n")) {
                message += "\n";
            }

            ByteBuffer writeBuffer = ByteBuffer.wrap(message.getBytes());
            while (writeBuffer.hasRemaining()) {
                channel.write(writeBuffer);
            }
        } catch (IOException e) {
            System.err.println("Failed to write to " + getRemoteAddress());
            // Client might be gone, this will be caught on next read
        }
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public String getRemoteAddress() {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "Unknown Client";
        }
    }
}