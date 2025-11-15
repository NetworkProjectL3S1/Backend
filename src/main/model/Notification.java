package main.model;

/**
 * Notification model for storing user notifications
 */
public class Notification {
    private long notificationId;
    private String username;
    private String type; // BID_PLACED, BID_WON, AUCTION_EXPIRED, NEW_MESSAGE
    private String title;
    private String message;
    private String auctionId;
    private long timestamp;
    private boolean isRead;

    public Notification() {
    }

    public Notification(String username, String type, String title, String message, String auctionId) {
        this.username = username;
        this.type = type;
        this.title = title;
        this.message = message;
        this.auctionId = auctionId;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Getters
    public long getNotificationId() {
        return notificationId;
    }

    public String getUsername() {
        return username;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    // Setters
    public void setNotificationId(long notificationId) {
        this.notificationId = notificationId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", username='" + username + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", auctionId='" + auctionId + '\'' +
                ", timestamp=" + timestamp +
                ", isRead=" + isRead +
                '}';
    }
}
