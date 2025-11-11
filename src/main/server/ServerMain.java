package main.server;

public class ServerMain {

    public static void main(String[] args) {
        // Define the port to listen on
        int port = 9999;

        try {
            // Create and start the main auction server
            AuctionServer server = new AuctionServer(port);
            System.out.println("Attempting to start Auction Server on port " + port);
            server.start();

        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}