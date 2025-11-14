# User Authentication System - Setup Guide

## Overview

This project now includes a complete user authentication system with:
- **Backend**: Java-based REST API with SQLite database for persistent user storage
- **Frontend**: React-based authentication UI with login/register forms
- **Security**: Password hashing (SHA-256) and token-based authentication

## Backend Authentication Setup

### Database Schema

The system automatically creates a `users` table in SQLite (`data/auction_system.db`):

```sql
CREATE TABLE users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,        -- SHA-256 hashed
    email TEXT,
    role TEXT NOT NULL DEFAULT 'BUYER',  -- 'BUYER' or 'SELLER'
    token TEXT,                    -- Session token
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

### API Endpoints

All endpoints are available at `http://localhost:8081/api/auth/`

#### 1. Register User
**POST** `/api/auth/register`

Request body:
```json
{
  "username": "john_doe",
  "password": "securepass123",
  "email": "john@example.com",
  "role": "BUYER"
}
```

Response (201 Created):
```json
{
  "success": true,
  "message": "User registered successfully",
  "token": "uuid-timestamp",
  "user": {
    "username": "john_doe",
    "email": "john@example.com",
    "role": "BUYER"
  }
}
```

#### 2. Login User
**POST** `/api/auth/login`

Request body:
```json
{
  "username": "john_doe",
  "password": "securepass123"
}
```

Response (200 OK):
```json
{
  "success": true,
  "message": "Login successful",
  "token": "uuid-timestamp",
  "user": {
    "username": "john_doe",
    "email": "john@example.com",
    "role": "BUYER"
  }
}
```

#### 3. Verify Token
**POST** `/api/auth/verify`

Request body:
```json
{
  "token": "uuid-timestamp"
}
```

Response (200 OK):
```json
{
  "valid": true,
  "user": {
    "username": "john_doe",
    "email": "john@example.com",
    "role": "BUYER"
  }
}
```

### Starting the Backend

1. **Compile the project**:
```bash
cd Backend-1
./compile.sh
```

2. **Start the API server**:
```bash
./start-api-server.sh
```

The API server will start on port `8081` and automatically:
- Initialize the SQLite database
- Create the users table
- Start accepting authentication requests

## Frontend Authentication Setup

### Components

1. **AuthContext** (`src/contexts/AuthContext.jsx`): Manages authentication state
2. **authService** (`src/utils/authService.js`): Handles API calls to backend
3. **LoginForm** (`src/components/LoginForm.jsx`): UI for login/registration

### Features

- **Login/Register Toggle**: Switch between login and registration modes
- **Role Selection**: Choose between BUYER and SELLER roles during registration
- **Token Persistence**: Auth token stored in localStorage
- **Auto-verification**: Verifies token on page load
- **Protected Routes**: Main app only accessible when authenticated

### Starting the Frontend

```bash
cd Frontend-1/auction-client
npm install
npm run dev
```

The frontend will start on `http://localhost:5173`

## Usage Flow

### New User Registration

1. Open the frontend app
2. Click "Create Account"
3. Enter username, password, and optional email
4. Select role (BUYER or SELLER)
5. Click "Sign Up"
6. Automatically logged in and redirected to auction list

### Existing User Login

1. Open the frontend app
2. Enter username and password
3. Click "Sign In"
4. Redirected to auction list

### Demo Credentials

For testing, use the demo user:
- **Username**: `demo_user`
- **Password**: `demo_pass`

Click "Fill Demo Credentials" button on login form.

## Database Management

### View Database

```bash
cd Backend-1
sqlite3 data/auction_system.db

# List all users
SELECT * FROM users;

# Count users
SELECT COUNT(*) FROM users;

# Users by role
SELECT role, COUNT(*) FROM users GROUP BY role;
```

### Backup Database

```bash
cp data/auction_system.db data/backups/auction_system_backup_$(date +%Y%m%d_%H%M%S).db
```

## Security Notes

### Current Implementation

- Passwords are hashed using SHA-256
- Tokens are UUID-based with timestamps
- CORS enabled for frontend-backend communication

### Production Recommendations

1. **Use BCrypt**: Replace SHA-256 with BCrypt for password hashing
2. **JWT Tokens**: Implement JSON Web Tokens instead of UUID
3. **HTTPS**: Use SSL/TLS in production
4. **Environment Variables**: Store sensitive config in env files
5. **Rate Limiting**: Add rate limiting for auth endpoints
6. **Token Expiry**: Implement token expiration and refresh

## Testing Authentication

### Backend Testing

Test registration:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123456","email":"test@test.com","role":"BUYER"}'
```

Test login:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123456"}'
```

### Frontend Testing

1. **Registration**:
   - Fill form with new user details
   - Verify user appears in database
   - Check console for API responses

2. **Login**:
   - Use existing credentials
   - Verify token in localStorage
   - Check user context is populated

3. **Logout**:
   - Click logout button
   - Verify token removed from localStorage
   - Redirected to login page

## Troubleshooting

### Backend Issues

**Database not created**:
- Check `data/` directory exists
- Verify SQLite JDBC driver in classpath
- Check server console for errors

**Port 8081 in use**:
```bash
lsof -i :8081
kill -9 <PID>
```

### Frontend Issues

**Cannot connect to backend**:
- Verify backend is running on port 8081
- Check CORS headers in browser console
- Verify API_BASE_URL in authService.js

**Token not persisting**:
- Check browser localStorage
- Clear cache and cookies
- Verify authService.js saves token

## Files Modified/Created

### Backend Files
- ✨ **NEW**: `src/main/api/controllers/AuthController.java` - Authentication endpoints
- ✅ **MODIFIED**: `src/main/util/DatabaseManager.java` - Added user auth methods
- ✅ **MODIFIED**: `src/main/model/User.java` - Added auth fields
- ✅ **MODIFIED**: `src/main/api/ApiServer.java` - Registered auth routes

### Frontend Files
- ✅ **MODIFIED**: `src/utils/authService.js` - Updated API URL
- ✅ **EXISTING**: `src/contexts/AuthContext.jsx` - Auth state management
- ✅ **EXISTING**: `src/components/LoginForm.jsx` - Login/Register UI

## Next Steps

1. **Integrate with Auctions**: Link user authentication to auction creation/bidding
2. **Add User Profiles**: Create user profile management
3. **Role-based Access**: Implement seller-only auction creation
4. **Password Reset**: Add forgot password functionality
5. **Email Verification**: Implement email confirmation

## Support

For issues or questions:
1. Check server console logs
2. Verify database contents
3. Test API endpoints with curl
4. Check browser console for errors
