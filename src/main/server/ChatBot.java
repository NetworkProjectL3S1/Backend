package main.server;

import main.model.Command;
import main.model.Message;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Chat bot logic and automated responses
 */
public class ChatBot {
    private static final String BOT_NAME = "ChatBot";
    private final Random random = new Random();
    
    // Bot responses for different scenarios
    private final List<String> greetingResponses = Arrays.asList(
        "Hello! How can I help you today?",
        "Hi there! Welcome to the chat!",
        "Hey! Nice to meet you!",
        "Greetings! I'm here to assist you.",
        "Hello! Feel free to ask me anything!"
    );
    
    private final List<String> helpResponses = Arrays.asList(
        "I'm here to help! You can chat with me or use commands.",
        "Need assistance? Just type /help to see available commands.",
        "I can respond to your messages and help with various commands!",
        "Feel free to talk to me or use commands like /time, /users, etc."
    );
    
    private final List<String> farewellResponses = Arrays.asList(
        "Goodbye! Thanks for chatting!",
        "See you later!",
        "Farewell! Come back soon!",
        "Bye! It was nice talking to you!",
        "Take care! Hope to see you again!"
    );
    
    private final List<String> unknownResponses = Arrays.asList(
        "I'm not sure I understand. Could you rephrase that?",
        "Hmm, I don't quite get what you mean. Can you try again?",
        "That's interesting! Can you tell me more?",
        "I'm still learning. Could you explain that differently?",
        "Sorry, I didn't catch that. What do you mean?"
    );
    
    /**
     * Process a command and generate appropriate response
     */
    public Message processCommand(Command command, String username) {
        UserManager userManager = UserManager.getInstance();
        
        switch (command.getType()) {
            case HELP:
                return new Message(
                    Message.MessageType.BOT_RESPONSE,
                    BOT_NAME,
                    Command.getHelpText()
                );
                
            case USERS:
                return new Message(
                    Message.MessageType.BOT_RESPONSE,
                    BOT_NAME,
                    userManager.getUserListForDisplay()
                );
                
            case TIME:
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentTime = formatter.format(new Date());
                return new Message(
                    Message.MessageType.BOT_RESPONSE,
                    BOT_NAME,
                    "Current server time: " + currentTime
                );
                
            case BOT:
                String userMessage = command.getParametersAsString();
                return new Message(
                    Message.MessageType.BOT_RESPONSE,
                    BOT_NAME,
                    generateBotResponse(userMessage, username)
                );
                
            case PRIVATE_MESSAGE:
                return handlePrivateMessage(command, username);
                
            case QUIT:
                return new Message(
                    Message.MessageType.SYSTEM,
                    BOT_NAME,
                    "User " + username + " has left the chat."
                );
                
            case HISTORY:
                return handleHistoryCommand(command, username);
                
            case STATS:
                return handleStatsCommand(username);
                
            case SEARCH:
                return handleSearchCommand(command, username);
                
            default:
                return new Message(
                    Message.MessageType.BOT_RESPONSE,
                    BOT_NAME,
                    "Unknown command: " + command.getRawCommand() + ". Type /help for available commands."
                );
        }
    }
    
    /**
     * Generate a bot response to user messages
     */
    public String generateBotResponse(String userMessage, String username) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Hello " + username + "! " + getRandomResponse(helpResponses);
        }
        
        String message = userMessage.toLowerCase().trim();
        
        // Greeting patterns
        if (message.matches(".*(hello|hi|hey|greetings?).*")) {
            return "Hello " + username + "! " + getRandomResponse(greetingResponses);
        }
        
        // Farewell patterns
        if (message.matches(".*(bye|goodbye|farewell|see you|cya).*")) {
            return getRandomResponse(farewellResponses);
        }
        
        // Help patterns
        if (message.matches(".*(help|assist|support).*")) {
            return getRandomResponse(helpResponses);
        }
        
        // Time-related queries
        if (message.matches(".*(time|clock|date).*")) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            return "The current time is " + formatter.format(new Date());
        }
        
        // Weather (mock response)
        if (message.matches(".*(weather|temperature|forecast).*")) {
            return "I wish I could check the weather for you, but I don't have access to weather data. Try checking a weather website!";
        }
        
        // Bot information
        if (message.matches(".*(who are you|what are you|your name).*")) {
            return "I'm " + BOT_NAME + ", a simple chat bot built in Java! I can respond to messages and help with various commands.";
        }
        
        // Fun responses
        if (message.matches(".*(joke|funny|laugh).*")) {
            return getFunnyResponse();
        }
        
        // Math operations (simple)
        if (message.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*")) {
            return handleMathOperation(message);
        }
        
        // Default response with some personality
        return "That's interesting, " + username + "! " + getRandomResponse(unknownResponses);
    }
    
    /**
     * Handle private message command
     */
    private Message handlePrivateMessage(Command command, String senderUsername) {
        String[] params = command.getParameters();
        if (params.length < 2) {
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                "Usage: /pm <username> <message>"
            );
        }
        
        String targetUsername = params[0];
        String messageContent = String.join(" ", Arrays.copyOfRange(params, 1, params.length));
        
        UserManager userManager = UserManager.getInstance();
        if (!userManager.isUserActive(targetUsername)) {
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                "User '" + targetUsername + "' is not online."
            );
        }
        
        // Create private message
        Message privateMsg = new Message(
            Message.MessageType.MESSAGE,
            senderUsername,
            "[Private] " + messageContent,
            targetUsername
        );
        
        return privateMsg;
    }
    
    /**
     * Handle simple math operations
     */
    private String handleMathOperation(String message) {
        try {
            // Extract numbers and operator (very basic implementation)
            String[] parts = message.replaceAll("[^\\d+\\-*/\\s]", "").trim().split("\\s+");
            if (parts.length >= 3) {
                double num1 = Double.parseDouble(parts[0]);
                String operator = parts[1];
                double num2 = Double.parseDouble(parts[2]);
                
                double result;
                switch (operator) {
                    case "+":
                        result = num1 + num2;
                        break;
                    case "-":
                        result = num1 - num2;
                        break;
                    case "*":
                        result = num1 * num2;
                        break;
                    case "/":
                        if (num2 == 0) return "Cannot divide by zero!";
                        result = num1 / num2;
                        break;
                    default:
                        return "I can help with basic math: +, -, *, /";
                }
                
                return String.format("%.2f %s %.2f = %.2f", num1, operator, num2, result);
            }
        } catch (NumberFormatException e) {
            // Ignore and fall through to default
        }
        
        return "I can help with simple math operations like: 5 + 3, 10 * 2, etc.";
    }
    
    /**
     * Get a funny response
     */
    private String getFunnyResponse() {
        String[] jokes = {
            "Why do programmers prefer dark mode? Because light attracts bugs! 🐛",
            "How many programmers does it take to change a light bulb? None, that's a hardware problem!",
            "Why do Java developers wear glasses? Because they can't C#! 😄",
            "I told my computer a joke about UDP, but I'm not sure if it got it...",
            "There are only 10 types of people: those who understand binary and those who don't!"
        };
        
        return jokes[random.nextInt(jokes.length)];
    }
    
    /**
     * Get a random response from a list
     */
    private String getRandomResponse(List<String> responses) {
        return responses.get(random.nextInt(responses.size()));
    }
    
    /**
     * Generate a system welcome message for new users
     */
    public Message generateWelcomeMessage(String username) {
        return new Message(
            Message.MessageType.SYSTEM,
            BOT_NAME,
            "Welcome to the chat, " + username + "! Type /help to see available commands, or just start chatting!"
        );
    }
    
    /**
     * Generate a system goodbye message
     */
    public Message generateGoodbyeMessage(String username) {
        return new Message(
            Message.MessageType.SYSTEM,
            BOT_NAME,
            username + " has left the chat. " + getRandomResponse(farewellResponses)
        );
    }
    
    /**
     * Handle history command to show recent messages
     */
    private Message handleHistoryCommand(Command command, String username) {
        int messageCount = 10; // default
        
        String[] params = command.getParameters();
        if (params.length > 0) {
            try {
                messageCount = Integer.parseInt(params[0]);
                if (messageCount < 1 || messageCount > 50) {
                    return new Message(
                        Message.MessageType.BOT_RESPONSE,
                        BOT_NAME,
                        "Please specify a number between 1 and 50 for message history."
                    );
                }
            } catch (NumberFormatException e) {
                return new Message(
                    Message.MessageType.BOT_RESPONSE,
                    BOT_NAME,
                    "Invalid number format. Usage: /history [number]"
                );
            }
        }
        
        try {
            main.util.MessageStorage storage = main.util.MessageStorage.getInstance();
            java.util.List<main.model.Message> recentMessages = storage.getRecentMessages(messageCount);
            
            if (recentMessages.isEmpty()) {
                return new Message(
                    Message.MessageType.BOT_RESPONSE,
                    BOT_NAME,
                    "No message history available yet."
                );
            }
            
            StringBuilder history = new StringBuilder();
            history.append("📜 Recent ").append(recentMessages.size()).append(" messages:\n");
            
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            for (main.model.Message msg : recentMessages) {
                String time = formatter.format(new java.util.Date(msg.getTimestamp()));
                history.append("[").append(time).append("] ");
                
                if (msg.getType() == main.model.Message.MessageType.BOT_RESPONSE) {
                    history.append("[Bot] ").append(msg.getContent());
                } else if (msg.getTargetUser() != null) {
                    history.append("<").append(msg.getUsername()).append(" -> ").append(msg.getTargetUser()).append("> ");
                    history.append(msg.getContent());
                } else {
                    history.append("<").append(msg.getUsername()).append("> ").append(msg.getContent());
                }
                history.append("\n");
            }
            
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                history.toString().trim()
            );
            
        } catch (Exception e) {
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                "Error retrieving message history: " + e.getMessage()
            );
        }
    }
    
    /**
     * Handle stats command to show storage statistics
     */
    private Message handleStatsCommand(String username) {
        try {
            main.util.MessageStorage storage = main.util.MessageStorage.getInstance();
            String stats = storage.getStorageStats();
            
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                "📊 " + stats
            );
            
        } catch (Exception e) {
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                "Error retrieving storage statistics: " + e.getMessage()
            );
        }
    }
    
    /**
     * Handle search command to find messages by user
     */
    private Message handleSearchCommand(Command command, String username) {
        String[] params = command.getParameters();
        if (params.length == 0) {
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                "Usage: /search <username> - Find messages by a specific user"
            );
        }
        
        String searchUsername = params[0];
        
        try {
            main.util.MessageStorage storage = main.util.MessageStorage.getInstance();
            java.util.List<main.model.Message> userMessages = storage.getMessagesByUser(searchUsername);
            
            if (userMessages.isEmpty()) {
                return new Message(
                    Message.MessageType.BOT_RESPONSE,
                    BOT_NAME,
                    "No messages found for user: " + searchUsername
                );
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🔍 Found ").append(userMessages.size()).append(" messages from ").append(searchUsername).append(":\n");
            
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            int displayCount = Math.min(userMessages.size(), 10); // Show max 10 messages
            
            for (int i = 0; i < displayCount; i++) {
                main.model.Message msg = userMessages.get(i);
                String time = formatter.format(new java.util.Date(msg.getTimestamp()));
                result.append("[").append(time).append("] ").append(msg.getContent()).append("\n");
            }
            
            if (userMessages.size() > 10) {
                result.append("... and ").append(userMessages.size() - 10).append(" more messages");
            }
            
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                result.toString().trim()
            );
            
        } catch (Exception e) {
            return new Message(
                Message.MessageType.BOT_RESPONSE,
                BOT_NAME,
                "Error searching messages: " + e.getMessage()
            );
        }
    }
}
