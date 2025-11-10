package main.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages thread pools for the chat server
 */
public class ThreadPoolManager {
    private static ThreadPoolManager instance;
    private ExecutorService clientHandlerPool;
    private ExecutorService messageProcessorPool;
    
    private ThreadPoolManager() {
        // Create thread pools
        clientHandlerPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ClientHandler-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        messageProcessorPool = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "MessageProcessor-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }
    
    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }
    
    /**
     * Submit a client handler task
     */
    public void submitClientHandler(Runnable task) {
        clientHandlerPool.submit(task);
    }
    
    /**
     * Submit a message processing task
     */
    public void submitMessageProcessor(Runnable task) {
        messageProcessorPool.submit(task);
    }
    
    /**
     * Shutdown all thread pools gracefully
     */
    public void shutdown() {
        System.out.println("Shutting down thread pools...");
        
        clientHandlerPool.shutdown();
        messageProcessorPool.shutdown();
        
        try {
            if (!clientHandlerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                clientHandlerPool.shutdownNow();
            }
            if (!messageProcessorPool.awaitTermination(10, TimeUnit.SECONDS)) {
                messageProcessorPool.shutdownNow();
            }
            System.out.println("Thread pools shutdown complete.");
        } catch (InterruptedException e) {
            clientHandlerPool.shutdownNow();
            messageProcessorPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get active thread count for client handlers
     */
    public int getActiveClientHandlers() {
        return ((java.util.concurrent.ThreadPoolExecutor) clientHandlerPool).getActiveCount();
    }
    
    /**
     * Get active thread count for message processors
     */
    public int getActiveMessageProcessors() {
        return ((java.util.concurrent.ThreadPoolExecutor) messageProcessorPool).getActiveCount();
    }
    
    /**
     * Check if thread pools are shutdown
     */
    public boolean isShutdown() {
        return clientHandlerPool.isShutdown() && messageProcessorPool.isShutdown();
    }
}
