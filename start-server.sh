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

# Start the server
java -cp build main.server.ChatServer $PORT
