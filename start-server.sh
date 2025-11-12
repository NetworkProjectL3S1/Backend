#!/bin/bash

# Start the Chat Server

echo "Starting Java WebSocket Chat Server..."

# Default port
PORT=${1:-8080}

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

# Start the server
java -cp "${CLASSPATH}" main.server.ServerMain $PORT
