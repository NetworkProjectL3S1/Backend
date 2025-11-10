#!/bin/bash

# Compile the Java Chat Server Project

echo "Compiling Java WebSocket Chat Server..."

# Create build directory if it doesn't exist
mkdir -p build

# Compile all Java files
find src -name "*.java" -print0 | xargs -0 javac -d build -cp build

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "To run the server:"
    echo "  java -cp build main.server.ChatServer [port]"
    echo ""
    echo "To run a test client:"
    echo "  java -cp build main.client.ChatClient [host] [port]"
    echo ""
    echo "To run tests:"
    echo "  java -cp build test.ChatServerTest"
else
    echo "❌ Compilation failed!"
    exit 1
fi
