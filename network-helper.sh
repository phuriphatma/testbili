#!/bin/bash

# iPad PDF Viewer - Network Access Helper
# This script helps you find the correct URL for iPad access

echo "🌐 iPad PDF Viewer - Network Access"
echo "===================================="
echo ""

# Check if npm is available
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed or not in PATH"
    exit 1
fi

# Get the local IP address
LOCAL_IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1)

if [ -z "$LOCAL_IP" ]; then
    echo "❌ Could not determine local IP address"
    echo "   Make sure you're connected to Wi-Fi"
    exit 1
fi

echo "📱 Your Mac's IP address: $LOCAL_IP"
echo "🚀 iPad Access URL: http://$LOCAL_IP:5173"
echo ""
echo "Instructions:"
echo "1. Make sure your iPad is on the same Wi-Fi network"
echo "2. Open Safari on your iPad"
echo "3. Navigate to: http://$LOCAL_IP:5173"
echo "4. Bookmark the URL for easy access"
echo ""

# Check if the development server is running
if lsof -i :5173 > /dev/null 2>&1; then
    echo "✅ Development server is running on port 5173"
else
    echo "⚠️  Development server is NOT running"
    echo "   Run 'npm run dev' to start the server"
fi

echo ""
echo "🔧 To start the development server:"
echo "   npm run dev"
echo ""
echo "📋 Quick copy commands:"
echo "   echo 'http://$LOCAL_IP:5173' | pbcopy"
echo "   (URL copied to clipboard)"
