#!/bin/bash

# Quick API Test Script
# This script tests the REST API endpoints

API_URL="http://localhost:8081/api"

echo "=========================================="
echo "  Auction System API - Quick Test"
echo "=========================================="
echo ""

# Check if server is running
echo "1. Testing health endpoint..."
HEALTH=$(curl -s $API_URL/health)
if [[ $HEALTH == *"ok"* ]]; then
    echo "   ✅ Server is running!"
else
    echo "   ❌ Server is not running. Please start it with: ./start-api-server.sh"
    exit 1
fi
echo ""

# Create a test auction
echo "2. Creating test auction..."
CREATE_RESPONSE=$(curl -s -X POST $API_URL/auctions/create \
  -H "Content-Type: application/json" \
  -d '{
    "itemName": "Test Item",
    "itemDescription": "This is a test auction",
    "sellerId": "testuser",
    "basePrice": 100,
    "duration": 60,
    "category": "test"
  }')

if [[ $CREATE_RESPONSE == *"success\":true"* ]]; then
    echo "   ✅ Auction created successfully!"
    # Extract auction ID (simple grep)
    AUCTION_ID=$(echo $CREATE_RESPONSE | grep -o '"auctionId":"[^"]*"' | cut -d'"' -f4)
    echo "   Auction ID: $AUCTION_ID"
else
    echo "   ❌ Failed to create auction"
    echo "   Response: $CREATE_RESPONSE"
    exit 1
fi
echo ""

# List auctions
echo "3. Listing all auctions..."
LIST_RESPONSE=$(curl -s $API_URL/auctions/list)
if [[ $LIST_RESPONSE == *"success\":true"* ]]; then
    echo "   ✅ Successfully retrieved auction list!"
else
    echo "   ❌ Failed to list auctions"
    exit 1
fi
echo ""

# Place a bid
if [ ! -z "$AUCTION_ID" ]; then
    echo "4. Placing a bid..."
    BID_RESPONSE=$(curl -s -X POST $API_URL/bids/place \
      -H "Content-Type: application/json" \
      -d "{
        \"auctionId\": \"$AUCTION_ID\",
        \"userId\": \"bidder123\",
        \"amount\": 150
      }")
    
    if [[ $BID_RESPONSE == *"success\":true"* ]]; then
        echo "   ✅ Bid placed successfully!"
    else
        echo "   ❌ Failed to place bid"
        echo "   Response: $BID_RESPONSE"
    fi
    echo ""
    
    # Get bid history
    echo "5. Getting bid history..."
    HISTORY_RESPONSE=$(curl -s "$API_URL/bids/history?auctionId=$AUCTION_ID")
    if [[ $HISTORY_RESPONSE == *"success\":true"* ]]; then
        echo "   ✅ Successfully retrieved bid history!"
    else
        echo "   ❌ Failed to get bid history"
    fi
fi

echo ""
echo "=========================================="
echo "  All API tests completed!"
echo "=========================================="
echo ""
echo "For complete API documentation, see:"
echo "  - API_DOCUMENTATION.md"
echo "  - React examples in react-example/"
echo ""
