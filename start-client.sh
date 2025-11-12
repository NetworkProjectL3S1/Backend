#!/bin/bash

# Start a Chat Client

echo "Starting Java WebSocket Chat Client..."

# Default connection settings
HOST=${1:-localhost}
PORT=${2:-8080}

# Check if compiled
if [ ! -d "build" ]; then
    echo "Project not compiled. Running compile script..."
    ./compile.sh
fi

# Find SQLite JDBC driver
SQLITE_JAR=$(find lib -name "sqlite-jdbc-*.jar" 2>/dev/null | head -n 1)

if [ -z "$SQLITE_JAR" ]; then
    echo "⚠️  SQLite JDBC driver not found in lib directory"
    echo "Run './download-dependencies.sh' to download required dependencies"
    exit 1
fi

# Set classpath
CLASSPATH="build:${SQLITE_JAR}"

echo "Connecting to $HOST:$PORT..."
echo "Use Ctrl+C to exit if connection fails"

# Start the client
java -cp "${CLASSPATH}" main.client.ChatClient $HOST $PORT
