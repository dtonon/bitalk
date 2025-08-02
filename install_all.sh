#!/bin/bash

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "=== Installing Bitalk on all connected devices ==="
echo ""

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: APK not found at $APK_PATH"
    echo "Please run: ./gradlew assembleDebug"
    exit 1
fi

# Get list of connected devices
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | cut -f1)

if [ -z "$DEVICES" ]; then
    echo "No devices connected. Please connect your devices and enable USB debugging."
    exit 1
fi

echo "Connected devices:"
adb devices
echo ""

# Install on each device
for device in $DEVICES; do
    echo "Installing on device: $device"
    adb -s $device install -r "$APK_PATH"
    if [ $? -eq 0 ]; then
        echo "✓ Successfully installed on $device"
    else
        echo "✗ Failed to install on $device"
    fi
    echo ""
done

echo "Installation complete!"