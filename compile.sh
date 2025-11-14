#!/bin/bash

# Compile the Java Chat Server Project

echo "Compiling Java WebSocket Chat Server..."

# Create build directory if it doesn't exist
mkdir -p build

# Set up classpath with MySQL driver if available
MYSQL_JAR="lib/mysql-connector-j-8.2.0.jar"
if [ -f "$MYSQL_JAR" ]; then
    CLASSPATH="build:$MYSQL_JAR"
    echo "📦 Using MySQL JDBC driver: $MYSQL_JAR"
else
    CLASSPATH="build"
    echo "⚠️  MySQL JDBC driver not found. Run ./setup-mysql.sh first for database support."
fi

# Compile all Java files
find src -name "*.java" -print0 | xargs -0 javac -d build -cp "$CLASSPATH"

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    if [ -f "$MYSQL_JAR" ]; then
        echo "🗄️  Database support: MySQL (enabled)"
        echo "To run the server:"
        echo "  java -cp \"build:$MYSQL_JAR\" main.server.ChatServer [port]"
        echo ""
        echo "To run a test client:"
        echo "  java -cp \"build:$MYSQL_JAR\" main.client.ChatClient [host] [port]"
    else
        echo "📁 Database support: File storage (fallback)"
        echo "To run the server:"
        echo "  java -cp build main.server.ChatServer [port]"
        echo ""
        echo "To run a test client:"
        echo "  java -cp build main.client.ChatClient [host] [port]"
    fi
    echo ""
    echo "To run tests:"
    echo "  java -cp \"$CLASSPATH\" test.ChatServerTest"
else
    echo "❌ Compilation failed!"
    exit 1
fi
