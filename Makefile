# Makefile for Java WebSocket Chat Server
# =======================================

JAVA_FILES = $(shell find src -name "*.java")
BUILD_DIR = build
MAIN_CLASS = main.server.ChatServer
CLIENT_CLASS = main.client.ChatClient
TEST_CLASS = test.ChatServerTest

# Default target
all: compile

# Compile all Java files
compile:
	@echo "Compiling Java WebSocket Chat Server..."
	@mkdir -p $(BUILD_DIR)
	@find src -name "*.java" -print0 | xargs -0 javac -d $(BUILD_DIR) -cp $(BUILD_DIR)
	@echo "✅ Compilation successful!"

# Run the server
server: compile
	@echo "Starting Chat Server..."
	java -cp $(BUILD_DIR) $(MAIN_CLASS)

# Run the server with custom port
server-port: compile
	@echo "Starting Chat Server on port $(PORT)..."
	java -cp $(BUILD_DIR) $(MAIN_CLASS) $(PORT)

# Run a test client
client: compile
	@echo "Starting Chat Client..."
	java -cp $(BUILD_DIR) $(CLIENT_CLASS)

# Run tests
test: compile
	@echo "Running tests..."
	java -cp $(BUILD_DIR) $(TEST_CLASS)

# Clean build directory
clean:
	@echo "Cleaning build directory..."
	@rm -rf $(BUILD_DIR)
	@echo "✅ Clean complete!"

# Create project documentation
docs:
	@echo "Generating documentation..."
	@javadoc -d docs -cp $(BUILD_DIR) -sourcepath src $(JAVA_FILES)
	@echo "✅ Documentation generated in docs/ directory"

# Package the application
package: compile
	@echo "Creating JAR file..."
	@mkdir -p dist
	@jar cfm dist/chat-server.jar manifest.txt -C $(BUILD_DIR) .
	@cp config.properties dist/
	@echo "✅ JAR file created: dist/chat-server.jar"

# Install dependencies (none for this pure Java project)
install:
	@echo "No external dependencies to install."
	@echo "This project uses pure Java with no external libraries."

# Run multiple clients for testing
multi-client: compile
	@echo "Starting multiple test clients..."
	@for i in 1 2 3; do \
		echo "Starting client $$i..."; \
		gnome-terminal -- bash -c "java -cp $(BUILD_DIR) $(CLIENT_CLASS); exec bash" 2>/dev/null || \
		osascript -e 'tell app "Terminal" to do script "cd $(PWD) && java -cp $(BUILD_DIR) $(CLIENT_CLASS)"' 2>/dev/null || \
		echo "Could not open new terminal for client $$i"; \
	done

# Display project information
info:
	@echo "Java WebSocket Chat Server"
	@echo "=========================="
	@echo "Main class: $(MAIN_CLASS)"
	@echo "Client class: $(CLIENT_CLASS)"
	@echo "Test class: $(TEST_CLASS)"
	@echo "Build directory: $(BUILD_DIR)"
	@echo ""
	@echo "Available targets:"
	@echo "  compile     - Compile all Java files"
	@echo "  server      - Start the chat server"
	@echo "  client      - Start a test client"
	@echo "  test        - Run tests"
	@echo "  clean       - Clean build directory"
	@echo "  docs        - Generate documentation"
	@echo "  package     - Create JAR file"
	@echo "  multi-client- Start multiple test clients"
	@echo "  info        - Show this information"

# Help target
help: info

.PHONY: all compile server server-port client test clean docs package install multi-client info help
