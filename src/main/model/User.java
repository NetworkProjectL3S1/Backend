package main.model;

/**
 * Represents a user in the chat system
 */
public class User {
    private String username;
    private String password;
    private String email;
    private String role;
    private String token;
    private String sessionId;
    private long connectTime;
    private boolean isActive;
    
    public User(String username, String sessionId) {
        this.username = username;
        this.sessionId = sessionId;
        this.connectTime = System.currentTimeMillis();
        this.isActive = true;
    }
    
    public User(String username, String password, String email, String role, String token) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.token = token;
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
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

    public static class User1 {
        private final String username;
        private final String password;
        private boolean isLoggedIn;

        public User1(String username, String password) {
            this.username = username;
            this.password = password;
            this.isLoggedIn = false;
        }

        // --- Getters and Setters ---
        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public boolean isLoggedIn() {
            return isLoggedIn;
        }

        public void setLoggedIn(boolean loggedIn) {
            isLoggedIn = loggedIn;
        }
    }
}
