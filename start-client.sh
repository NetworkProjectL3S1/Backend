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

echo "Connecting to $HOST:$PORT..."
echo "Use Ctrl+C to exit if connection fails"

# Start the client
java -cp build main.client.ChatClient $HOST $PORT
