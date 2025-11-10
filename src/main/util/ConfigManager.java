package main.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration manager for the chat server
 */
public class ConfigManager {
    private static ConfigManager instance;
    private final Properties properties;
    
    private ConfigManager() {
        properties = new Properties();
        loadDefaultConfig();
        loadConfigFile();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadDefaultConfig() {
        // Default configuration values
        properties.setProperty("server.port", "8080");
        properties.setProperty("server.max.connections", "100");
        properties.setProperty("server.thread.pool.size", "10");
        properties.setProperty("websocket.max.frame.size", "8192");
        properties.setProperty("chat.bot.response.probability", "0.1");
        properties.setProperty("chat.bot.response.delay.min", "1000");
        properties.setProperty("chat.bot.response.delay.max", "3000");
        properties.setProperty("user.username.max.length", "20");
        properties.setProperty("message.max.length", "500");
        properties.setProperty("server.name", "Java WebSocket Chat Server");
    }
    
    private void loadConfigFile() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                System.out.println("Configuration loaded from config.properties");
            }
        } catch (IOException e) {
            System.out.println("Using default configuration (config.properties not found)");
        }
    }
    
    public String getString(String key) {
        return properties.getProperty(key);
    }
    
    public int getInt(String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for key: " + key);
            return 0;
        }
    }
    
    public double getDouble(String key) {
        try {
            return Double.parseDouble(properties.getProperty(key));
        } catch (NumberFormatException e) {
            System.err.println("Invalid double value for key: " + key);
            return 0.0;
        }
    }
    
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    public void printConfiguration() {
        System.out.println("Current Configuration:");
        System.out.println("=====================");
        properties.forEach((key, value) -> 
            System.out.println(key + " = " + value));
        System.out.println("=====================");
    }
}
