package main.server;

import main.model.User;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Manages user sessions and connections
 */
public class UserManager {
    private static UserManager instance;
    private static final String USER_FILE = "data/users/users.dat";
    private final ConcurrentMap<String, User> persistentUsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClientHandler> userConnections = new ConcurrentHashMap<>();
    
    private UserManager() {
        loadUsersFromFile();
    }
    
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    private void loadUsersFromFile() {
        File file = new File(USER_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                // Read the entire map back
                Map<String, User> loadedMap = (Map<String, User>) ois.readObject();
                persistentUsers.putAll(loadedMap);
                System.out.println("Loaded " + persistentUsers.size() + " users from " + USER_FILE);
            } catch (FileNotFoundException e) {
                // Should not happen as we check file.exists()
                System.err.println("User file not found: " + USER_FILE);
            } catch (EOFException e) {
                // File is empty, starting fresh
                System.out.println("User file is empty. Starting with an empty user list.");
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading users from file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("User file not found. Creating data directory and starting fresh.");
            new File(file.getParent()).mkdirs(); // Create the directory
        }
    }

    private synchronized void saveUsersToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(persistentUsers);
            System.out.println("Successfully saved " + persistentUsers.size() + " users to " + USER_FILE);
        } catch (IOException e) {
            System.err.println("Error saving users to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public User registerUser(String username, String password) {
        return registerUser(username, password, main.model.User.Role.BUYER);
    }

    public User registerUser(String username, String password, main.model.User.Role role) {
        username = username.toLowerCase();
        if (persistentUsers.containsKey(username)) {
            return null; // Username already exists
        }

        User newUser = new User(username, password, role);
        persistentUsers.put(username, newUser);
        saveUsersToFile();
        return newUser;
    }

    public User authenticateUser(String username, String password) {
        username = username.toLowerCase();
        User user = persistentUsers.get(username);

        if (user != null && user.getPassword().equals(password)) {
            if (userConnections.containsKey(username)) {
                // User is already logged in on another connection
                return null;
            }
            return user; // Authentication successful
        }
        return null; // Authentication failed
    }

    /**
     * Adds an active connection for an authenticated user.
     */
    public void addActiveUser(User user, ClientHandler clientHandler) {
        userConnections.put(user.getUsername(), clientHandler);
        user.setLoggedIn(true);
        System.out.println("User logged in: " + user.getUsername() + " (Active connections: " + userConnections.size() + ")");
    }

    /**
     * Removes an active user connection.
     */
    public void removeActiveUser(String username) {
        ClientHandler handler = userConnections.remove(username);
        User user = persistentUsers.get(username);
        if (user != null) {
            user.setLoggedIn(false);
            System.out.println("User logged out: " + username + " (Active connections: " + userConnections.size() + ")");
        }
    }

    // ... (other session methods remain similar, but now reference userConnections map)

    /**
     * Check if username is already logged in
     */
    public boolean isUserLoggedIn(String username) {
        return userConnections.containsKey(username);
    }
    
    /**
     * Add a new user to the system
     */
    public boolean addUser(User user, ClientHandler clientHandler) {
        if (users.containsKey(user.getUsername())) {
            return false; // Username already taken
        }
        
        users.put(user.getUsername(), user);
        userConnections.put(user.getUsername(), clientHandler);
        System.out.println("User added: " + user.getUsername() + " (Total users: " + users.size() + ")");
        return true;
    }
    
    /**
     * Remove a user from the system
     */
    public void removeUser(String username) {
        User removedUser = users.remove(username);
        userConnections.remove(username);
        if (removedUser != null) {
            removedUser.setActive(false);
            System.out.println("User removed: " + username + " (Total users: " + users.size() + ")");
        }
    }
    
    /**
     * Get a user by username (from active users)
     */
    public User getUser(String username) {
        return users.get(username);
    }
    
    /**
     * Get a persistent user by username (from registered users)
     */
    public User getPersistentUser(String username) {
        return persistentUsers.get(username != null ? username.toLowerCase() : null);
    }
    
    /**
     * Get client handler for a specific user
     */
    public ClientHandler getClientHandler(String username) {
        return userConnections.get(username);
    }
    
    /**
     * Get all active users
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    /**
     * Get all active usernames
     */
    public List<String> getAllUsernames() {
        return users.values().stream()
                .filter(User::isActive)
                .map(User::getUsername)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all client handlers
     */
    public List<ClientHandler> getAllClientHandlers() {
        return new ArrayList<>(userConnections.values());
    }
    
    /**
     * Check if username is available
     */
    public boolean isUsernameAvailable(String username) {
        return !users.containsKey(username);
    }
    
    /**
     * Get total number of connected users
     */
    public int getUserCount() {
        return users.size();
    }
    
    /**
     * Check if user exists and is active
     */
    public boolean isUserActive(String username) {
        User user = users.get(username);
        return user != null && user.isActive();
    }
    
    /**
     * Get formatted list of all users for display
     */
    public String getUserListForDisplay() {
        if (users.isEmpty()) {
            return "No users currently connected.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Connected Users (").append(users.size()).append("):\n");
        
        for (User user : users.values()) {
            if (user.isActive()) {
                long connectionTime = (System.currentTimeMillis() - user.getConnectTime()) / 1000;
                sb.append("• ").append(user.getUsername())
                  .append(" (connected for ").append(connectionTime).append("s)\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Clear all users (for shutdown)
     */
    public void clearAllUsers() {
        users.clear();
        userConnections.clear();
        System.out.println("All users cleared from UserManager");
    }
}
