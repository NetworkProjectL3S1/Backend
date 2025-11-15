# Makefile for Java WebSocket Chat Server
# =======================================

JAVA_FILES = $(shell find src -name "*.java")
BUILD_DIR = build
LIB_DIR = lib
SQLITE_JAR = $(shell find $(LIB_DIR) -name "sqlite-jdbc-*.jar" 2>/dev/null | head -n 1)
CLASSPATH = $(BUILD_DIR):$(SQLITE_JAR)

MAIN_CLASS = main.server.ServerMain
CLIENT_CLASS = main.client.ChatClient
TEST_CLASS = test.ChatServerTest
DB_TEST_CLASS = main.test.DatabaseTest

# Default target
all: deps compile

# Download dependencies
deps:
	@if [ ! -f "$(SQLITE_JAR)" ]; then \
		echo "Downloading dependencies..."; \
		./download-dependencies.sh; \
	else \
		echo "✅ Dependencies already present"; \
	fi

# Compile all Java files
compile: deps
	@echo "Compiling Java WebSocket Chat Server..."
	@mkdir -p $(BUILD_DIR)
	@if [ -z "$(SQLITE_JAR)" ]; then \
		echo "❌ SQLite JDBC driver not found. Run 'make deps' first."; \
		exit 1; \
	fi
	@find src -name "*.java" -print0 | xargs -0 javac -d $(BUILD_DIR) -cp "$(CLASSPATH)"
	@echo "✅ Compilation successful!"
	@echo "Using classpath: $(CLASSPATH)"

# Run the server
server: compile
	@echo "Starting Chat Server..."
	java -cp "$(CLASSPATH)" $(MAIN_CLASS)

# Run the server with custom port
server-port: compile
	@echo "Starting Chat Server on port $(PORT)..."
	java -cp "$(CLASSPATH)" $(MAIN_CLASS) $(PORT)

# Run a test client
client: compile
	@echo "Starting Chat Client..."
	java -cp "$(CLASSPATH)" $(CLIENT_CLASS)

# Run tests
test: compile
	@echo "Running tests..."
	java -cp "$(CLASSPATH)" $(TEST_CLASS)

# Run database tests
test-db: compile
	@echo "Running database tests..."
	java -cp "$(CLASSPATH)" $(DB_TEST_CLASS)

# Clean build directory
clean:
	@echo "Cleaning build directory..."
	@rm -rf $(BUILD_DIR)
	@echo "✅ Clean complete!"

# Clean all (including dependencies and data)
clean-all: clean
	@echo "Cleaning all generated files..."
	@rm -rf $(LIB_DIR) data dist docs
	@echo "✅ Complete clean!"

# Create project documentation
docs:
	@echo "Generating documentation..."
	@javadoc -d docs -cp $(BUILD_DIR) -sourcepath src $(JAVA_FILES)

# Create project documentation
docs:
	@echo "Generating documentation..."
	@javadoc -d docs -cp "$(CLASSPATH)" -sourcepath src $(JAVA_FILES)
	@echo "✅ Documentation generated in docs/ directory"

# Package the application
package: compile
	@echo "Creating JAR file..."
	@mkdir -p dist
	@jar cfm dist/chat-server.jar manifest.txt -C $(BUILD_DIR) .
	@cp config.properties dist/ 2>/dev/null || true
	@cp -r $(LIB_DIR) dist/ 2>/dev/null || true
	@echo "✅ JAR file created: dist/chat-server.jar"
	@echo "   Dependencies copied to dist/lib/"

# Install dependencies
install: deps
	@echo "✅ All dependencies installed"

# Backup database
backup-db:
	@echo "Backing up database..."
	@mkdir -p data/backups
	@if [ -f "data/auction_system.db" ]; then \
		cp data/auction_system.db data/backups/auction_backup_$$(date +%Y%m%d_%H%M%S).db; \
		echo "✅ Database backed up to data/backups/"; \
	else \
		echo "⚠️  No database found to backup"; \
	fi

# View database
db-shell:
	@if [ -f "data/auction_system.db" ]; then \
		sqlite3 data/auction_system.db; \
	else \
		echo "❌ Database not found. Run the server first to create it."; \
	fi

# Run multiple clients for testing
multi-client: compile
	@echo "Starting multiple test clients..."
	@for i in 1 2 3; do \
		echo "Starting client $$i..."; \
		gnome-terminal -- bash -c "java -cp \"$(CLASSPATH)\" $(CLIENT_CLASS); exec bash" 2>/dev/null || \
		osascript -e "tell app \"Terminal\" to do script \"cd $(PWD) && java -cp \\\"$(CLASSPATH)\\\" $(CLIENT_CLASS)\"" 2>/dev/null || \
		echo "Could not open new terminal for client $$i"; \
	done

# Display project information
info:
	@echo "Java WebSocket Chat Server"
	@echo "=========================="
	@echo "Main class: $(MAIN_CLASS)"
	@echo "Client class: $(CLIENT_CLASS)"
	@echo "Test class: $(TEST_CLASS)"
	@echo "DB Test class: $(DB_TEST_CLASS)"
	@echo "Build directory: $(BUILD_DIR)"
	@echo "Classpath: $(CLASSPATH)"
	@echo ""
	@echo "Available targets:"
	@echo "  deps        - Download dependencies (SQLite JDBC)"
	@echo "  compile     - Compile all Java files"
	@echo "  server      - Start the chat server"
	@echo "  client      - Start a test client"
	@echo "  test        - Run tests"
	@echo "  test-db     - Run database tests"
	@echo "  clean       - Clean build directory"
	@echo "  clean-all   - Clean everything (build, deps, data)"
	@echo "  docs        - Generate documentation"
	@echo "  package     - Create JAR file with dependencies"
	@echo "  backup-db   - Backup the SQLite database"
	@echo "  db-shell    - Open SQLite database shell"
	@echo "  multi-client- Start multiple test clients"
	@echo "  info        - Show this information"

# Help target
help: info

.PHONY: all deps compile server server-port client test test-db clean clean-all docs package install backup-db db-shell multi-client info help
