package main.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a user in the auction system with buyer/seller roles
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Role {
        BUYER,
        SELLER
    }

    private final String userId;
    private String username;
    private final String password;
    private Role role;
    private String sessionId;
    private long connectTime;
    private boolean isActive;
    private transient boolean isLoggedIn;

    public User(String username, String password) {
        this.userId = UUID.randomUUID().toString();
        this.username = username;
        this.password = password;
        this.role = Role.BUYER; // Default role
        this.isLoggedIn = false;
    }

    public User(String username, String password, Role role) {
        this.userId = UUID.randomUUID().toString();
        this.username = username;
        this.password = password;
        this.role = role != null ? role : Role.BUYER;
        this.isLoggedIn = false;
    }

    // Constructor for existing users (when loaded from file)
    private User(String userId, String username, String password) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.isLoggedIn = false;
    }
    
    public User(String userId, String username, String password, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = Role.BUYER; // Default role
        this.sessionId = sessionId;
        this.connectTime = System.currentTimeMillis();
        this.isActive = true;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public long getConnectTime() {
        return connectTime;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void setConnectTime(long connectTime) {
        this.connectTime = connectTime;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public String getPassword() {
        return password;
    }

    public String getUserId() {
        return userId;
    }

    public Role getRole() {
        // Handle backward compatibility: if role is null (from old serialized data), default to BUYER
        if (role == null) {
            role = Role.BUYER;
        }
        return role;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.BUYER;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", connectTime=" + connectTime +
                ", isActive=" + isActive +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return sessionId.equals(user.sessionId);
    }
    
    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}
