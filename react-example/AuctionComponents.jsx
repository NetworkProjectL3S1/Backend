// React Component Example for Auction System API Integration
// This is a sample React component showing how to integrate with the auction API

import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8081/api';

// Auction List Component
export const AuctionList = () => {
  const [auctions, setAuctions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchAuctions();
  }, []);

  const fetchAuctions = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/auctions/list`);
      const data = await response.json();
      
      if (data.success) {
        setAuctions(data.data);
      } else {
        setError(data.error);
      }
    } catch (err) {
      setError('Failed to fetch auctions: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading auctions...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="auction-list">
      <h2>Active Auctions</h2>
      {auctions.map(auction => (
        <AuctionCard key={auction.auctionId} auction={auction} />
      ))}
    </div>
  );
};

// Auction Card Component
const AuctionCard = ({ auction }) => {
  const [bidAmount, setBidAmount] = useState('');
  const [bidError, setBidError] = useState('');
  const [bidSuccess, setBidSuccess] = useState('');

  const handlePlaceBid = async (e) => {
    e.preventDefault();
    setBidError('');
    setBidSuccess('');

    const amount = parseFloat(bidAmount);
    
    if (amount <= auction.currentHighestBid) {
      setBidError(`Bid must be higher than current bid ($${auction.currentHighestBid})`);
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/bids/place`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          auctionId: auction.auctionId,
          userId: 'currentUser', // Replace with actual user ID from auth
          amount: amount
        })
      });

      const data = await response.json();

      if (data.success) {
        setBidSuccess('Bid placed successfully!');
        setBidAmount('');
        // Refresh auction data
        window.location.reload();
      } else {
        setBidError(data.error);
      }
    } catch (err) {
      setBidError('Failed to place bid: ' + err.message);
    }
  };

  const timeRemaining = Math.max(0, auction.endTime - Date.now());
  const hours = Math.floor(timeRemaining / (1000 * 60 * 60));
  const minutes = Math.floor((timeRemaining % (1000 * 60 * 60)) / (1000 * 60));

  return (
    <div className="auction-card">
      <h3>{auction.itemName}</h3>
      <p>{auction.itemDescription}</p>
      <div className="auction-details">
        <p><strong>Category:</strong> {auction.category}</p>
        <p><strong>Seller:</strong> {auction.sellerId}</p>
        <p><strong>Starting Price:</strong> ${auction.basePrice.toFixed(2)}</p>
        <p><strong>Current Bid:</strong> ${auction.currentHighestBid.toFixed(2)}</p>
        {auction.currentHighestBidder && (
          <p><strong>Highest Bidder:</strong> {auction.currentHighestBidder}</p>
        )}
        <p><strong>Time Remaining:</strong> {hours}h {minutes}m</p>
        <p><strong>Status:</strong> {auction.status}</p>
      </div>
      
      {auction.status === 'ACTIVE' && (
        <form onSubmit={handlePlaceBid} className="bid-form">
          <input
            type="number"
            step="0.01"
            placeholder="Enter bid amount"
            value={bidAmount}
            onChange={(e) => setBidAmount(e.target.value)}
            min={auction.currentHighestBid + 0.01}
            required
          />
          <button type="submit">Place Bid</button>
        </form>
      )}
      
      {bidError && <p className="error">{bidError}</p>}
      {bidSuccess && <p className="success">{bidSuccess}</p>}
    </div>
  );
};

// Create Auction Form Component
export const CreateAuctionForm = () => {
  const [formData, setFormData] = useState({
    itemName: '',
    itemDescription: '',
    sellerId: '', // Should come from authenticated user
    basePrice: '',
    duration: '60',
    category: 'general'
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      const response = await fetch(`${API_BASE_URL}/auctions/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          itemName: formData.itemName,
          itemDescription: formData.itemDescription,
          sellerId: formData.sellerId,
          basePrice: parseFloat(formData.basePrice),
          duration: parseInt(formData.duration),
          category: formData.category
        })
      });

      const data = await response.json();

      if (data.success) {
        setSuccess('Auction created successfully!');
        // Reset form
        setFormData({
          itemName: '',
          itemDescription: '',
          sellerId: formData.sellerId, // Keep seller ID
          basePrice: '',
          duration: '60',
          category: 'general'
        });
      } else {
        setError(data.error);
      }
    } catch (err) {
      setError('Failed to create auction: ' + err.message);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  return (
    <div className="create-auction-form">
      <h2>Create New Auction</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Item Name:</label>
          <input
            type="text"
            name="itemName"
            value={formData.itemName}
            onChange={handleChange}
            required
          />
        </div>

        <div className="form-group">
          <label>Description:</label>
          <textarea
            name="itemDescription"
            value={formData.itemDescription}
            onChange={handleChange}
            rows="4"
          />
        </div>

        <div className="form-group">
          <label>Seller ID:</label>
          <input
            type="text"
            name="sellerId"
            value={formData.sellerId}
            onChange={handleChange}
            required
          />
        </div>

        <div className="form-group">
          <label>Starting Price:</label>
          <input
            type="number"
            step="0.01"
            name="basePrice"
            value={formData.basePrice}
            onChange={handleChange}
            min="0.01"
            required
          />
        </div>

        <div className="form-group">
          <label>Duration (minutes):</label>
          <input
            type="number"
            name="duration"
            value={formData.duration}
            onChange={handleChange}
            min="1"
            required
          />
        </div>

        <div className="form-group">
          <label>Category:</label>
          <select
            name="category"
            value={formData.category}
            onChange={handleChange}
          >
            <option value="general">General</option>
            <option value="electronics">Electronics</option>
            <option value="collectibles">Collectibles</option>
            <option value="art">Art</option>
            <option value="vehicles">Vehicles</option>
            <option value="other">Other</option>
          </select>
        </div>

        <button type="submit">Create Auction</button>
      </form>

      {error && <p className="error">{error}</p>}
      {success && <p className="success">{success}</p>}
    </div>
  );
};

// Bid History Component
export const BidHistory = ({ auctionId }) => {
  const [bids, setBids] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchBidHistory();
  }, [auctionId]);

  const fetchBidHistory = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/bids/history?auctionId=${auctionId}`);
      const data = await response.json();
      
      if (data.success) {
        setBids(data.data);
      } else {
        setError(data.error);
      }
    } catch (err) {
      setError('Failed to fetch bid history: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading bid history...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="bid-history">
      <h3>Bid History</h3>
      {bids.length === 0 ? (
        <p>No bids yet</p>
      ) : (
        <ul>
          {bids.map((bid, index) => (
            <li key={index}>
              <strong>{bid.userId}</strong> bid <strong>${bid.amount.toFixed(2)}</strong>
              {' '}at {new Date(bid.timestamp).toLocaleString()}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

// Main App Component Example
export const AuctionApp = () => {
  const [activeTab, setActiveTab] = useState('browse');

  return (
    <div className="auction-app">
      <header>
        <h1>Auction System</h1>
        <nav>
          <button onClick={() => setActiveTab('browse')}>Browse Auctions</button>
          <button onClick={() => setActiveTab('create')}>Create Auction</button>
        </nav>
      </header>

      <main>
        {activeTab === 'browse' && <AuctionList />}
        {activeTab === 'create' && <CreateAuctionForm />}
      </main>
    </div>
  );
};

export default AuctionApp;
