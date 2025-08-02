#!/bin/bash

# Script to collect Bitalk debug logs from multiple devices
echo "=== Bitalk BLE Debug Logs ==="
echo ""

# Cleanup function to kill all background processes
cleanup() {
    echo ""
    echo "Stopping log collection..."
    # Kill all background jobs
    jobs -p | xargs -r kill
    # Kill any remaining adb logcat processes
    pkill -f "adb.*logcat" 2>/dev/null
    echo "Cleanup complete."
    exit 0
}

# Set trap to call cleanup on Ctrl+C
trap cleanup SIGINT SIGTERM

# Get list of connected devices
DEVICES=$(adb devices | grep -v "List of devices" | grep -E "(device|emulator)$" | cut -f1)

if [ -z "$DEVICES" ]; then
    echo "No devices connected."
    exit 1
fi

echo "Connected devices:"
adb devices
echo ""

# Let user choose device or monitor all
echo "Choose an option:"
echo "1) Monitor all devices simultaneously"
echo "2) Choose specific device"
echo ""
read -p "Enter choice (1 or 2): " choice

if [ "$choice" = "1" ]; then
    echo "Monitoring all devices. Use Ctrl+C to stop."
    echo ""
    
    # Clear logs on all devices
    for device in $DEVICES; do
        adb -s $device logcat -c 2>/dev/null
    done
    
    # Monitor all devices in parallel
    for device in $DEVICES; do
        {
            echo "=== Starting logs for device $device ==="
            adb -s $device logcat | grep -E "(MainActivity|OnboardingViewModel|MainViewModel|BitalkBLEService|TopicBroadcast|TopicMatcher)" | sed "s/^/[$device] /"
        } &
    done
    
    # Wait for Ctrl+C (cleanup will handle termination)
    wait
    
elif [ "$choice" = "2" ]; then
    echo ""
    echo "Available devices:"
    i=1
    for device in $DEVICES; do
        echo "$i) $device"
        i=$((i+1))
    done
    echo ""
    read -p "Enter device number: " device_num
    
    # Get selected device
    selected_device=$(echo $DEVICES | cut -d' ' -f$device_num)
    
    if [ -z "$selected_device" ]; then
        echo "Invalid device number."
        exit 1
    fi
    
    echo "Monitoring device: $selected_device"
    echo "Use Ctrl+C to stop."
    echo ""
    
    # Clear logs and start monitoring
    adb -s $selected_device logcat -c
    adb -s $selected_device logcat | grep -E "(MainActivity|OnboardingViewModel|MainViewModel|BitalkBLEService|TopicBroadcast|TopicMatcher)"
    
else
    echo "Invalid choice."
    exit 1
fi