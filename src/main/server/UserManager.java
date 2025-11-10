package main.server;

import main.model.User;
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
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClientHandler> userConnections = new ConcurrentHashMap<>();
    
    private UserManager() {}
    
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
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
     * Get a user by username
     */
    public User getUser(String username) {
        return users.get(username);
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
