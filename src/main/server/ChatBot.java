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
}
