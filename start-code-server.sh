#!/bin/bash
# Code-server startup script with extensions

echo "Starting code-server with GitHub Copilot..."

# Kill any existing code-server processes
pkill -f code-server

# Wait a moment
sleep 2

# Start code-server in background
nohup code-server /Users/xeno/webdev > code-server.log 2>&1 &

echo "Code-server started!"
echo "Access it at: http://192.168.1.182:8080"
echo "Password: ipad123"
echo ""
echo "Installed extensions:"
code-server --list-extensions
