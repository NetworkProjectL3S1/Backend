#!/bin/bash

# Quick JSON Format Test
# Shows the correct API response format

echo "=========================================="
echo "  API Response Format Example"
echo "=========================================="
echo ""

echo "✅ CORRECT FORMAT for /api/auctions/list:"
echo ""
cat << 'EOF'
{
  "success": true,
  "data": [
    {
      "auctionId": "auction-1699776000000-123",
      "itemName": "Vintage Watch",
      "itemDescription": "Beautiful 1960s watch",
      "sellerId": "user123",
      "basePrice": 100.00,
      "currentHighestBid": 150.00,
      "currentHighestBidder": "user456",
      "status": "ACTIVE",
      "category": "collectibles",
      "createdTime": 1699776000000,
      "endTime": 1699779600000,
      "duration": 3600000
    },
    {
      "auctionId": "auction-1699776100000-456",
      "itemName": "Gaming Laptop",
      "itemDescription": "High-end gaming laptop",
      "sellerId": "user789",
      "basePrice": 1500.00,
      "currentHighestBid": 1500.00,
      "currentHighestBidder": null,
      "status": "ACTIVE",
      "category": "electronics",
      "createdTime": 1699776100000,
      "endTime": 1699779700000,
      "duration": 3600000
    }
  ]
}
EOF

echo ""
echo "=========================================="
echo "  React Usage Example"
echo "=========================================="
echo ""
cat << 'EOF'
// Fetch auctions in React
const fetchAuctions = async () => {
  const response = await fetch('http://localhost:8081/api/auctions/list');
  const data = await response.json();
  
  if (data.success) {
    // data.data is an array of auction objects
    const auctions = data.data;
    console.log('Auctions:', auctions);
    
    // Use directly
    auctions.forEach(auction => {
      console.log(auction.itemName, auction.currentHighestBid);
    });
    
    // Or set state in React
    setAuctions(auctions);
  }
};
EOF

echo ""
echo "=========================================="
echo ""
echo "The fix ensures that the API returns:"
echo "  - success: true/false"
echo "  - data: array of auction objects (not a string!)"
echo ""
