package main.model;

/**
 * Represents a command that can be executed by the chat bot
 */
public class Command {
    public enum CommandType {
        HELP, USERS, TIME, BOT, QUIT, PRIVATE_MESSAGE, UNKNOWN
    }
    
    private CommandType type;
    private String[] parameters;
    private String rawCommand;
    
    public Command(String rawCommand) {
        this.rawCommand = rawCommand.trim();
        parseCommand();
    }
    
    private void parseCommand() {
        if (!rawCommand.startsWith("/")) {
            type = CommandType.UNKNOWN;
            parameters = new String[0];
            return;
        }
        
        String[] parts = rawCommand.substring(1).split("\\s+");
        String commandName = parts[0].toLowerCase();
        
        // Extract parameters (everything after the command name)
        parameters = new String[parts.length - 1];
        System.arraycopy(parts, 1, parameters, 0, parameters.length);
        
        // Determine command type
        switch (commandName) {
            case "help":
                type = CommandType.HELP;
                break;
            case "users":
                type = CommandType.USERS;
                break;
            case "time":
                type = CommandType.TIME;
                break;
            case "bot":
                type = CommandType.BOT;
                break;
            case "quit":
            case "exit":
                type = CommandType.QUIT;
                break;
            case "pm":
            case "msg":
            case "whisper":
                type = CommandType.PRIVATE_MESSAGE;
                break;
            default:
                type = CommandType.UNKNOWN;
                break;
        }
    }
    
    public CommandType getType() {
        return type;
    }
    
    public String[] getParameters() {
        return parameters;
    }
    
    public String getRawCommand() {
        return rawCommand;
    }
    
    /**
     * Get parameters as a single string (useful for bot messages)
     */
    public String getParametersAsString() {
        if (parameters.length == 0) {
            return "";
        }
        return String.join(" ", parameters);
    }
    
    /**
     * Check if command is valid
     */
    public boolean isValid() {
        return type != CommandType.UNKNOWN;
    }
    
    /**
     * Get help text for all available commands
     */
    public static String getHelpText() {
        return "Available commands:\n" +
               "/help - Show this help message\n" +
               "/users - List all connected users\n" +
               "/time - Get current server time\n" +
               "/bot <message> - Send a message to the chat bot\n" +
               "/pm <username> <message> - Send a private message to a user\n" +
               "/quit - Disconnect from the chat server";
    }
    
    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", parameters=" + String.join(", ", parameters) +
                ", rawCommand='" + rawCommand + '\'' +
                '}';
    }
}
