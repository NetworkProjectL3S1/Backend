package main.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Test client for creating and managing auctions
 * Part of Module 2: Auction Creation
 */
public class AuctionCreatorClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = true;

    public AuctionCreatorClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        System.out.println("Connected to Auction Server at " + host + ":" + port);
    }

    public void start() {
        // Start reader thread
        Thread readerThread = new Thread(this::readMessages);
        readerThread.setDaemon(true);
        readerThread.start();

        // Display menu and handle user input
        displayMenu();
        handleUserInput();
    }

    private void displayMenu() {
        System.out.println("\n=== AUCTION CREATOR CLIENT ===");
        System.out.println("Commands:");
        System.out.println("  1. create   - Create a new auction");
        System.out.println("  2. list     - List all active auctions");
        System.out.println("  3. listcat  - List auctions by category");
        System.out.println("  4. details  - Get auction details");
        System.out.println("  5. watch    - Watch an auction");
        System.out.println("  6. bid      - Place a bid");
        System.out.println("  7. help     - Show this menu");
        System.out.println("  8. quit     - Exit");
        System.out.println("===============================\n");
    }

    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);

        while (running) {
            try {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                String[] parts = input.split("\\s+", 2);
                String command = parts[0].toLowerCase();

                switch (command) {
                    case "1":
                    case "create":
                        createAuction(scanner);
                        break;

                    case "2":
                    case "list":
                        listAuctions();
                        break;

                    case "3":
                    case "listcat":
                        listAuctionsByCategory(scanner);
                        break;

                    case "4":
                    case "details":
                        getAuctionDetails(scanner);
                        break;

                    case "5":
                    case "watch":
                        watchAuction(scanner);
                        break;

                    case "6":
                    case "bid":
                        placeBid(scanner);
                        break;

                    case "7":
                    case "help":
                        displayMenu();
                        break;

                    case "8":
                    case "quit":
                    case "exit":
                        running = false;
                        break;

                    default:
                        System.out.println("Unknown command. Type 'help' for available commands.");
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
        disconnect();
    }

    private void createAuction(Scanner scanner) {
        System.out.println("\n--- Create New Auction ---");

        System.out.print("Item Name: ");
        String itemName = scanner.nextLine().trim();

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();

        System.out.print("Seller ID (your username): ");
        String sellerId = scanner.nextLine().trim();

        System.out.print("Base Price: $");
        double basePrice = Double.parseDouble(scanner.nextLine().trim());

        System.out.print("Duration (minutes): ");
        long duration = Long.parseLong(scanner.nextLine().trim());

        System.out.print("Category (e.g., electronics, art, collectibles): ");
        String category = scanner.nextLine().trim();

        // Send CREATE_AUCTION command
        // Format:
        // CREATE_AUCTION:itemName:description:sellerId:basePrice:durationMinutes:category
        String command = String.format("CREATE_AUCTION:%s:%s:%s:%.2f:%d:%s",
                itemName, description, sellerId, basePrice, duration, category);

        sendMessage(command);
        System.out.println("Creating auction...");
    }

    private void listAuctions() {
        sendMessage("LIST_AUCTIONS");
        System.out.println("Fetching active auctions...");
    }

    private void listAuctionsByCategory(Scanner scanner) {
        System.out.print("Enter category: ");
        String category = scanner.nextLine().trim();
        sendMessage("LIST_AUCTIONS:" + category);
        System.out.println("Fetching auctions in category: " + category);
    }

    private void getAuctionDetails(Scanner scanner) {
        System.out.print("Enter auction ID (e.g., auction-1): ");
        String auctionId = scanner.nextLine().trim();
        sendMessage("GET_AUCTION:" + auctionId);
    }

    private void watchAuction(Scanner scanner) {
        System.out.print("Enter auction ID to watch: ");
        String auctionId = scanner.nextLine().trim();
        sendMessage("WATCH:" + auctionId);
    }

    private void placeBid(Scanner scanner) {
        System.out.print("Enter auction ID: ");
        String auctionId = scanner.nextLine().trim();

        System.out.print("Enter bid amount: $");
        double amount = Double.parseDouble(scanner.nextLine().trim());

        sendMessage("BID:" + auctionId + ":" + amount);
    }

    private void sendMessage(String message) {
        out.println(message);
    }

    private void readMessages() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                processServerMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Connection lost: " + e.getMessage());
                running = false;
            }
        }
    }

    private void processServerMessage(String message) {
        // Parse and display server messages
        if (message.startsWith("NEW_AUCTION:")) {
            displayNewAuction(message);
        } else if (message.startsWith("AUCTION_CREATED:")) {
            displayAuctionCreated(message);
        } else if (message.startsWith("AUCTION_DETAILS:")) {
            displayAuctionDetails(message);
        } else if (message.startsWith("NEW_BID:")) {
            displayNewBid(message);
        } else if (message.startsWith("ERROR:")) {
            System.out.println("\n❌ " + message.substring(6));
        } else if (message.startsWith("OK:")) {
            System.out.println("\n✓ " + message.substring(3));
        } else if (message.startsWith("CONFIRM:")) {
            System.out.println("\n✓ " + message.substring(8));
        } else if (message.equals("NO_AUCTIONS")) {
            System.out.println("\nNo auctions found.");
        } else if (message.equals("AUCTIONS_LIST_END")) {
            System.out.println("--- End of auction list ---\n");
        } else if (message.startsWith("AUCTIONS_LIST:")) {
            // Just a header, ignore or display
            System.out.println("\n--- " + message.substring(14) + " ---");
        } else {
            System.out.println("\n[Server] " + message);
        }
    }

    private void displayNewAuction(String message) {
        // Format:
        // NEW_AUCTION:auctionId:itemName:description:basePrice:timeRemaining:category:sellerId
        String[] parts = message.split(":", 8);
        if (parts.length >= 8) {
            System.out.println("\n🆕 NEW AUCTION CREATED!");
            System.out.println("  ID: " + parts[1]);
            System.out.println("  Item: " + parts[2]);
            System.out.println("  Description: " + parts[3]);
            System.out.println("  Base Price: $" + parts[4]);
            System.out.println("  Time Remaining: " + parts[5] + " seconds");
            System.out.println("  Category: " + parts[6]);
            System.out.println("  Seller: " + parts[7]);
            System.out.println();
        }
    }

    private void displayAuctionCreated(String message) {
        // Format: AUCTION_CREATED:auctionId:itemName
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            System.out.println("\n✓ Auction Created Successfully!");
            System.out.println("  Auction ID: " + parts[1]);
            System.out.println("  Item: " + parts[2]);
            System.out.println();
        }
    }

    private void displayAuctionDetails(String message) {
        // Format:
        // AUCTION_DETAILS:auctionId:itemName:description:basePrice:currentBid:highestBidder:timeRemaining:category:status
        String[] parts = message.split(":", 10);
        if (parts.length >= 10) {
            System.out.println("\n📋 AUCTION DETAILS");
            System.out.println("  ID: " + parts[1]);
            System.out.println("  Item: " + parts[2]);
            System.out.println("  Description: " + parts[3]);
            System.out.println("  Base Price: $" + parts[4]);
            System.out.println("  Current Bid: $" + parts[5]);
            System.out.println("  Highest Bidder: " + parts[6]);
            System.out.println("  Time Remaining: " + parts[7] + " seconds");
            System.out.println("  Category: " + parts[8]);
            System.out.println("  Status: " + parts[9]);
            System.out.println();
        }
    }

    private void displayNewBid(String message) {
        // Format: NEW_BID:auctionId:userId:amount
        String[] parts = message.split(":", 4);
        if (parts.length >= 4) {
            System.out.println("\n💰 NEW BID on " + parts[1]);
            System.out.println("  Bidder: " + parts[2]);
            System.out.println("  Amount: $" + parts[3]);
            System.out.println();
        }
    }

    private void disconnect() {
        try {
            running = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8081;

        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }

        try {
            AuctionCreatorClient client = new AuctionCreatorClient(host, port);
            client.start();
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
    }
}
