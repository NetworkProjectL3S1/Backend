# REST API Documentation

## Overview

This REST API provides endpoints for managing auctions and bids in the auction system. The API is designed to work with React frontends and stores all data in a SQLite database.

**Base URL:** `http://localhost:8081/api`

## CORS Support

All endpoints support CORS (Cross-Origin Resource Sharing) with:
- Allow-Origin: `*`
- Allow-Methods: `GET, POST, PUT, DELETE, OPTIONS`
- Allow-Headers: `Content-Type, Authorization`

## Endpoints

### Health Check

#### GET /api/health
Check if the API server is running.

**Response:**
```json
{
  "status": "ok",
  "timestamp": 1699776000000
}
```

---

### Auctions

#### POST /api/auctions/create
Create a new auction.

**Request Body:**
```json
{
  "itemName": "Vintage Watch",
  "itemDescription": "A beautiful vintage watch from the 1960s",
  "sellerId": "user123",
  "basePrice": 100.00,
  "duration": 60,
  "category": "collectibles"
}
```

**Fields:**
- `itemName` (required): Name of the item being auctioned
- `itemDescription` (optional): Detailed description of the item
- `sellerId` (required): ID of the user creating the auction
- `basePrice` (required): Starting price for the auction
- `duration` (required): Auction duration in minutes
- `category` (optional): Category of the item (default: "general")

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "auctionId": "auction-1699776000000-123",
    "itemName": "Vintage Watch",
    "itemDescription": "A beautiful vintage watch from the 1960s",
    "sellerId": "user123",
    "basePrice": 100.00,
    "currentHighestBid": 100.00,
    "currentHighestBidder": null,
    "status": "ACTIVE",
    "category": "collectibles",
    "createdTime": 1699776000000,
    "endTime": 1699779600000,
    "duration": 3600000
  }
}
```

#### GET /api/auctions/list
Get all auctions.

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "auctionId": "auction-1",
      "itemName": "Vintage Watch",
      "itemDescription": "A beautiful vintage watch",
      "sellerId": "user123",
      "basePrice": 100.00,
      "currentHighestBid": 150.00,
      "currentHighestBidder": "user456",
      "status": "ACTIVE",
      "category": "collectibles",
      "createdTime": 1699776000000,
      "endTime": 1699779600000,
      "duration": 3600000
    }
  ]
}
```

#### GET /api/auctions/{auctionId}
Get details of a specific auction.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "auctionId": "auction-1",
    "itemName": "Vintage Watch",
    ...
  }
}
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": "Auction not found"
}
```

---

### Bids

#### POST /api/bids/place
Place a bid on an auction.

**Request Body:**
```json
{
  "auctionId": "auction-1",
  "userId": "user456",
  "amount": 150.00
}
```

**Fields:**
- `auctionId` (required): ID of the auction to bid on
- `userId` (required): ID of the user placing the bid
- `amount` (required): Bid amount (must be higher than current highest bid)

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "auctionId": "auction-1",
    "userId": "user456",
    "amount": 150.00,
    "timestamp": 1699776100000
  }
}
```

**Error Responses:**
- `400 Bad Request`: Bid amount too low, auction not active, or auction expired
- `404 Not Found`: Auction not found

#### GET /api/bids/history?auctionId={auctionId}
Get bid history for an auction.

**Query Parameters:**
- `auctionId` (required): ID of the auction

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "auctionId": "auction-1",
      "userId": "user456",
      "amount": 150.00,
      "timestamp": 1699776100000
    },
    {
      "auctionId": "auction-1",
      "userId": "user789",
      "amount": 120.00,
      "timestamp": 1699776050000
    }
  ]
}
```

---

## Error Handling

All error responses follow this format:

```json
{
  "success": false,
  "error": "Error message description"
}
```

Common HTTP status codes:
- `200 OK`: Successful GET request
- `201 Created`: Successful POST request (resource created)
- `400 Bad Request`: Invalid request data
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

---

## React Integration Example

### Using Fetch API

```javascript
// Create an auction
const createAuction = async (auctionData) => {
  try {
    const response = await fetch('http://localhost:8081/api/auctions/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        itemName: auctionData.name,
        itemDescription: auctionData.description,
        sellerId: auctionData.sellerId,
        basePrice: parseFloat(auctionData.price),
        duration: parseInt(auctionData.duration),
        category: auctionData.category || 'general'
      })
    });
    
    const data = await response.json();
    
    if (data.success) {
      console.log('Auction created:', data.data);
      return data.data;
    } else {
      throw new Error(data.error);
    }
  } catch (error) {
    console.error('Error creating auction:', error);
    throw error;
  }
};

// Get all auctions
const getAllAuctions = async () => {
  try {
    const response = await fetch('http://localhost:8081/api/auctions/list');
    const data = await response.json();
    
    if (data.success) {
      return data.data;
    } else {
      throw new Error(data.error);
    }
  } catch (error) {
    console.error('Error fetching auctions:', error);
    throw error;
  }
};

// Place a bid
const placeBid = async (auctionId, userId, amount) => {
  try {
    const response = await fetch('http://localhost:8081/api/bids/place', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        auctionId,
        userId,
        amount: parseFloat(amount)
      })
    });
    
    const data = await response.json();
    
    if (data.success) {
      console.log('Bid placed:', data.data);
      return data.data;
    } else {
      throw new Error(data.error);
    }
  } catch (error) {
    console.error('Error placing bid:', error);
    throw error;
  }
};

// Get bid history
const getBidHistory = async (auctionId) => {
  try {
    const response = await fetch(`http://localhost:8081/api/bids/history?auctionId=${auctionId}`);
    const data = await response.json();
    
    if (data.success) {
      return data.data;
    } else {
      throw new Error(data.error);
    }
  } catch (error) {
    console.error('Error fetching bid history:', error);
    throw error;
  }
};
```

### Using Axios

```javascript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create auction
export const createAuction = async (auctionData) => {
  const response = await api.post('/auctions/create', auctionData);
  return response.data.data;
};

// Get all auctions
export const getAllAuctions = async () => {
  const response = await api.get('/auctions/list');
  return response.data.data;
};

// Place bid
export const placeBid = async (bidData) => {
  const response = await api.post('/bids/place', bidData);
  return response.data.data;
};

// Get bid history
export const getBidHistory = async (auctionId) => {
  const response = await api.get(`/bids/history?auctionId=${auctionId}`);
  return response.data.data;
};
```

---

## Running the API Server

### Start the API server:

```bash
# Compile the project
./compile.sh

# Run the API server
java -cp "bin:lib/*" main.api.ApiServer
```

The API will be available at `http://localhost:8081/api/`

### Test with curl:

```bash
# Health check
curl http://localhost:8081/api/health

# Create auction
curl -X POST http://localhost:8081/api/auctions/create \
  -H "Content-Type: application/json" \
  -d '{
    "itemName": "Test Item",
    "itemDescription": "Test Description",
    "sellerId": "user1",
    "basePrice": 100,
    "duration": 60,
    "category": "test"
  }'

# Get all auctions
curl http://localhost:8081/api/auctions/list

# Place bid
curl -X POST http://localhost:8081/api/bids/place \
  -H "Content-Type: application/json" \
  -d '{
    "auctionId": "auction-1",
    "userId": "user2",
    "amount": 150
  }'
```
