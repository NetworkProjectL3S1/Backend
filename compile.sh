#!/bin/bash

# Compile the Java Chat Server Project

echo "Compiling Java WebSocket Chat Server..."

# Create build directory if it doesn't exist
mkdir -p build

# Check if SQLite JDBC driver exists
SQLITE_JAR=$(find lib -name "sqlite-jdbc-*.jar" 2>/dev/null | head -n 1)

if [ -z "$SQLITE_JAR" ]; then
    echo "⚠️  SQLite JDBC driver not found in lib directory"
    echo "Run './download-dependencies.sh' to download required dependencies"
    exit 1
fi

# Set classpath
CLASSPATH="build:${SQLITE_JAR}"

echo "Using classpath: ${CLASSPATH}"

# Compile all Java files
find src -name "*.java" -print0 | xargs -0 javac -d build -cp "${CLASSPATH}"

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "To run the server:"
    echo "  java -cp \"${CLASSPATH}\" main.server.ServerMain [port]"
    echo ""
    echo "To run a test client:"
    echo "  java -cp \"${CLASSPATH}\" main.client.ChatClient [host] [port]"
    echo ""
    echo "To run tests:"
    echo "  java -cp \"${CLASSPATH}\" test.ChatServerTest"
else
    echo "❌ Compilation failed!"
    exit 1
fi
