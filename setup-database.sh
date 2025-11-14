#!/bin/bash

# Quick MySQL Database Setup Script
# This script helps you set up the MySQL database for the chat application

echo "🗄️  MySQL Database Setup for Chat Application"
echo "=============================================="

# Check if MySQL is running
if ! command -v mysql &> /dev/null; then
    echo "❌ MySQL command not found. Please install MySQL first."
    echo "💡 Install MySQL:"
    echo "   - macOS: brew install mysql"
    echo "   - Ubuntu: sudo apt install mysql-server"
    echo "   - Windows: Download from https://dev.mysql.com/downloads/"
    exit 1
fi

# Test MySQL connection
echo "🔌 Testing MySQL connection..."
if mysql -u root -p -e "SELECT 1;" &>/dev/null; then
    echo "✅ MySQL connection successful!"
else
    echo "❌ Cannot connect to MySQL. Please ensure:"
    echo "   1. MySQL server is running"
    echo "   2. Root user credentials are correct"
    echo "   3. Run: mysql -u root -p (to test connection manually)"
    exit 1
fi

echo ""
echo "📋 Setting up database..."
echo "Enter MySQL root password when prompted:"

# Execute the database setup SQL
mysql -u root -p < database-setup.sql

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 Database setup completed successfully!"
    echo ""
    echo "📊 What was created:"
    echo "   • Database: chatapp"
    echo "   • Table: messages (with optimized indexes)"
    echo "   • Table: users (for user tracking)"
    echo "   • Views: todays_messages, message_stats"
    echo ""
    echo "🚀 Next steps:"
    echo "   1. Update config.properties with your MySQL credentials"
    echo "   2. Run: ./start-server.sh"
    echo "   3. Your chat messages will now be saved to MySQL!"
    echo ""
    echo "💡 Test the connection with: mysql -u root -p chatapp"
else
    echo "❌ Database setup failed. Please check the error messages above."
    exit 1
fi
