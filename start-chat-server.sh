#!/bin/bash

# Start Chat Server
# This script compiles and runs the ChatServer on port 8080

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Starting Chat Server ===${NC}"

# Check if build directory exists
if [ ! -d "build/main" ]; then
    echo -e "${YELLOW}Build directory not found. Running compile.sh...${NC}"
    ./compile.sh
    if [ $? -ne 0 ]; then
        echo -e "${RED}Compilation failed. Exiting.${NC}"
        exit 1
    fi
fi

# Check if lib directory exists
if [ ! -d "lib" ]; then
    echo -e "${YELLOW}lib directory not found. Downloading dependencies...${NC}"
    ./download-dependencies.sh
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to download dependencies. Exiting.${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}Starting ChatServer on port 8080...${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop the server${NC}"
echo ""

# Start the chat server
java -cp "build:lib/*" main.server.ChatServer

# Capture exit code
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    echo -e "${RED}ChatServer exited with code $EXIT_CODE${NC}"
else
    echo -e "${GREEN}ChatServer stopped.${NC}"
fi

exit $EXIT_CODE
