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

# Set up classpath with MySQL driver if available
MYSQL_JAR="lib/mysql-connector-j-8.2.0.jar"
if [ -f "$MYSQL_JAR" ]; then
    CLASSPATH="build:$MYSQL_JAR"
    echo "🗄️  Using MySQL database storage"
else
    CLASSPATH="build"
    echo "📁 Using file-based storage (MySQL driver not found)"
fi

# Start the server
java -cp "$CLASSPATH" main.server.ChatServer $PORT
