#!/bin/bash

# Script to download SQLite JDBC driver
# This creates a lib directory and downloads the SQLite JDBC driver

echo "Setting up dependencies for the Auction System..."

# Create lib directory if it doesn't exist
mkdir -p lib

# SQLite JDBC version
SQLITE_VERSION="3.47.1.0"
SQLITE_JAR="sqlite-jdbc-${SQLITE_VERSION}.jar"
SQLITE_URL="https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/${SQLITE_VERSION}/${SQLITE_JAR}"

# Download SQLite JDBC driver if not already present
if [ ! -f "lib/${SQLITE_JAR}" ]; then
    echo "Downloading SQLite JDBC driver version ${SQLITE_VERSION}..."
    curl -L -o "lib/${SQLITE_JAR}" "${SQLITE_URL}"
    
    if [ $? -eq 0 ]; then
        echo "✓ SQLite JDBC driver downloaded successfully to lib/${SQLITE_JAR}"
    else
        echo "✗ Failed to download SQLite JDBC driver"
        echo "Please download manually from: ${SQLITE_URL}"
        exit 1
    fi
else
    echo "✓ SQLite JDBC driver already exists at lib/${SQLITE_JAR}"
fi

echo ""
echo "Dependencies setup complete!"
echo "You can now compile the project using: ./compile.sh"
