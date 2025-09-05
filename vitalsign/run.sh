#!/bin/bash

# VitalSign Patient Manager - Run Script
# This script sets up the environment and runs the Android project

echo "=== VitalSign Patient Manager Setup ==="

# Set JAVA_HOME to Android Studio's bundled JDK
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
echo "✓ JAVA_HOME set to: $JAVA_HOME"

# Verify Java installation
if ! $JAVA_HOME/bin/java -version > /dev/null 2>&1; then
    echo "❌ Java not found. Please install Android Studio first."
    exit 1
fi

echo "✓ Java found: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"

# Check if Android SDK exists
if [ ! -d "$HOME/Library/Android/sdk" ]; then
    echo "❌ Android SDK not found. Please install Android Studio and SDK."
    exit 1
fi

echo "✓ Android SDK found"

# Make sure we're in the project directory
cd "$(dirname "$0")"

echo "=== Building Project ==="

# Clean and build the project
echo "Cleaning project..."
./gradlew clean

echo "Building project..."
./gradlew build

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "To run the app:"
    echo "1. Open Android Studio"
    echo "2. Open this project: $(pwd)"
    echo "3. Click the 'Run' button or use Ctrl+R (Cmd+R on Mac)"
    echo ""
    echo "Or connect an Android device and run:"
    echo "./gradlew installDebug"
else
    echo "❌ Build failed. Check the error messages above."
    exit 1
fi
