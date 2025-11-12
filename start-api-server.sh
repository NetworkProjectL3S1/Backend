#!/bin/bash

# Start the REST API Server for the Auction System

echo "Starting Auction System REST API Server..."
echo "=========================================="

# Check if compiled (check both bin and build directories)
if [ ! -d "bin" ] && [ ! -d "build" ]; then
    echo "Error: Project not compiled. Please run ./compile.sh first"
    exit 1
fi

# Determine which directory to use
if [ -d "build" ]; then
    CLASS_DIR="build"
elif [ -d "bin" ]; then
    CLASS_DIR="bin"
fi

# Check if SQLite library exists
if [ ! -f "lib/sqlite-jdbc-3.47.1.0.jar" ]; then
    echo "Error: SQLite JDBC driver not found. Running download script..."
    ./download-dependencies.sh
fi

# Create data directory if it doesn't exist
mkdir -p data/backups

echo ""
echo "Starting API Server on http://localhost:8081/api/"
echo "Press Ctrl+C to stop the server"
echo ""

# Run the API server
java -cp "$CLASS_DIR:lib/*" main.api.ApiServer
