#!/bin/bash

# Test script for auction expiration feature
# Creates an auction that expires in 30 seconds

API_BASE="http://localhost:8081/api"

# Login as seller
echo "Logging in as seller..."
LOGIN_RESPONSE=$(curl -s -X POST "${API_BASE}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"seller1","password":"password"}')

echo "Login response: $LOGIN_RESPONSE"

# Extract token (simple parsing)
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
echo "Token: $TOKEN"

# Create auction with 30 second duration (0.5 minutes)
echo ""
echo "Creating test auction (expires in 30 seconds)..."
CREATE_RESPONSE=$(curl -s -X POST "${API_BASE}/auctions" \
  -H "Content-Type: application/json" \
  -d '{
    "itemName": "Test Expiration Item",
    "itemDescription": "This auction expires in 30 seconds to test timer functionality",
    "sellerId": "seller1",
    "basePrice": 10.00,
    "durationMinutes": 0.5,
    "category": "Test"
  }')

echo "Create response: $CREATE_RESPONSE"

# Extract auction ID
AUCTION_ID=$(echo "$CREATE_RESPONSE" | grep -o '"auctionId":"[^"]*' | cut -d'"' -f4)
echo ""
echo "Test auction created: $AUCTION_ID"
echo "This auction will expire in 30 seconds!"
echo ""
echo "Open http://localhost:5173/auction/$AUCTION_ID in your browser to see:"
echo "  1. Live countdown timer"
echo "  2. Expired message after 30 seconds"
echo "  3. Real-time expiration notification"
echo "  4. Disabled bid panel when expired"
