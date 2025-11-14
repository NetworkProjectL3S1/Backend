#!/bin/bash

# MySQL JDBC Driver Setup Script
# Downloads and sets up MySQL Connector/J for the chat application

MYSQL_CONNECTOR_VERSION="8.2.0"
MYSQL_CONNECTOR_JAR="mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar"
MYSQL_CONNECTOR_URL="https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${MYSQL_CONNECTOR_VERSION}/${MYSQL_CONNECTOR_JAR}"

LIB_DIR="lib"
DRIVER_PATH="${LIB_DIR}/${MYSQL_CONNECTOR_JAR}"

echo "🔧 MySQL JDBC Driver Setup"
echo "=========================="

# Create lib directory if it doesn't exist
if [ ! -d "$LIB_DIR" ]; then
    echo "📁 Creating lib directory..."
    mkdir -p "$LIB_DIR"
fi

# Check if driver already exists
if [ -f "$DRIVER_PATH" ]; then
    echo "✅ MySQL JDBC driver already exists: $DRIVER_PATH"
    echo "📦 Version: $MYSQL_CONNECTOR_VERSION"
    exit 0
fi

echo "📥 Downloading MySQL JDBC Driver..."
echo "🔗 URL: $MYSQL_CONNECTOR_URL"

# Download the MySQL JDBC driver
if command -v curl &> /dev/null; then
    curl -L -o "$DRIVER_PATH" "$MYSQL_CONNECTOR_URL"
elif command -v wget &> /dev/null; then
    wget -O "$DRIVER_PATH" "$MYSQL_CONNECTOR_URL"
else
    echo "❌ Error: Neither curl nor wget is available"
    echo "💡 Please manually download the MySQL JDBC driver from:"
    echo "   $MYSQL_CONNECTOR_URL"
    echo "   And save it as: $DRIVER_PATH"
    exit 1
fi

# Verify download
if [ -f "$DRIVER_PATH" ]; then
    file_size=$(stat -f%z "$DRIVER_PATH" 2>/dev/null || stat -c%s "$DRIVER_PATH" 2>/dev/null)
    if [ "$file_size" -gt 100000 ]; then
        echo "✅ MySQL JDBC driver downloaded successfully!"
        echo "📦 File: $DRIVER_PATH"
        echo "📊 Size: $file_size bytes"
        echo ""
        echo "🎯 Next steps:"
        echo "1. Make sure MySQL is running on localhost:3306"
        echo "2. Create database: CREATE DATABASE chatapp;"
        echo "3. Update config.properties with your MySQL credentials"
        echo "4. Run: ./compile.sh to compile with MySQL support"
        echo "5. Run: ./start-server.sh to start the server"
    else
        echo "❌ Downloaded file appears to be incomplete (size: $file_size bytes)"
        rm -f "$DRIVER_PATH"
        exit 1
    fi
else
    echo "❌ Failed to download MySQL JDBC driver"
    exit 1
fi
