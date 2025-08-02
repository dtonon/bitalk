#!/bin/bash

# Script to analyze BLE debug logs and identify issues
echo "=== Bitalk BLE Issue Analyzer ==="
echo ""

# Cleanup function
cleanup() {
    echo ""
    echo "Stopping analysis..."
    jobs -p | xargs -r kill 2>/dev/null
    pkill -f "adb.*logcat" 2>/dev/null
    exit 0
}
trap cleanup SIGINT SIGTERM

# Get devices
DEVICES=$(adb devices | grep -v "List of devices" | grep -E "(device|emulator)$" | cut -f1)

if [ -z "$DEVICES" ]; then
    echo "No devices connected."
    exit 1
fi

echo "Connected devices:"
adb devices
echo ""

# Clear logs on all devices
for device in $DEVICES; do
    adb -s $device logcat -c 2>/dev/null
done

echo "🔍 INSTRUCTIONS FOR TESTING:"
echo "1. On BOTH phones, complete onboarding with common topics (e.g., 'bitcoin', 'crypto')"
echo "2. On BOTH phones, tap the PLAY button to start BLE scanning"
echo "3. Keep phones close together (1-2 meters)"
echo "4. Wait 30 seconds for discovery"
echo ""
echo "🚀 Starting BLE analysis... Press Ctrl+C to stop"
echo ""

# Start log analysis
{
    # Monitor all devices and analyze in real-time
    for device in $DEVICES; do
        {
            adb -s $device logcat | grep -E "(MainActivity|OnboardingViewModel|MainViewModel|BitalkBLEService|TopicBroadcast|TopicMatcher)" | while read line; do
                echo "[$device] $line"
                
                # Real-time analysis
                case "$line" in
                    *"Starting Bitalk BLE services for user"*)
                        echo "✅ [$device] BLE SERVICE STARTED"
                        ;;
                    *"Missing Bluetooth permissions"*)
                        echo "❌ [$device] ERROR: MISSING BLUETOOTH PERMISSIONS"
                        ;;
                    *"Bluetooth not enabled"*)
                        echo "❌ [$device] ERROR: BLUETOOTH NOT ENABLED"
                        ;;
                    *"Started advertising"*)
                        echo "📡 [$device] ADVERTISING STARTED"
                        ;;
                    *"Advertising failed"*)
                        echo "❌ [$device] ERROR: ADVERTISING FAILED"
                        ;;
                    *"Started BLE scan"*)
                        echo "🔍 [$device] SCANNING STARTED"
                        ;;
                    *"Scan failed"*)
                        echo "❌ [$device] ERROR: SCAN FAILED"
                        ;;
                    *"Discovered device"*)
                        echo "📱 [$device] DEVICE DISCOVERED"
                        ;;
                    *"Connected to"*)
                        echo "🔗 [$device] GATT CONNECTED"
                        ;;
                    *"GATT read request"*)
                        echo "📥 [$device] GATT READ REQUEST RECEIVED"
                        ;;
                    *"Sending broadcast data"*)
                        echo "📤 [$device] SENDING BROADCAST DATA"
                        ;;
                    *"Processing broadcast data"*)
                        echo "📥 [$device] PROCESSING RECEIVED DATA"
                        ;;
                    *"Received broadcast from"*)
                        echo "📨 [$device] BROADCAST RECEIVED"
                        ;;
                    *"Topic matching result"*)
                        if [[ "$line" == *"[]"* ]]; then
                            echo "❌ [$device] NO TOPICS MATCHED"
                        else
                            echo "✅ [$device] TOPICS MATCHED!"
                        fi
                        ;;
                    *"*** MATCH FOUND ***"*)
                        echo "🎉 [$device] 🎉 USER MATCH FOUND! 🎉"
                        ;;
                    *"Notifying delegate of new user"*)
                        echo "🔔 [$device] NOTIFYING UI OF NEW USER"
                        ;;
                    *"Notifying delegate of updated user"*)
                        echo "🔄 [$device] UPDATING EXISTING USER"
                        ;;
                    *"Removing stale user"*)
                        echo "🗑️ [$device] REMOVING STALE USER"
                        ;;
                    *"Cleaned up"*)
                        echo "🧹 [$device] CLEANUP COMPLETED"
                        ;;
                    *"NEW"*)
                        echo "🆕 [$device] NEW USER DETECTED"
                        ;;
                    *"UPDATED"*)
                        echo "🔄 [$device] EXISTING USER UPDATED"
                        ;;
                    *"Failed to parse broadcast"*)
                        echo "❌ [$device] ERROR: FAILED TO PARSE BROADCAST DATA"
                        ;;
                    *"Permission denied"*)
                        echo "❌ [$device] ERROR: PERMISSION DENIED"
                        ;;
                esac
            done
        } &
    done
    
    # Wait for processes
    wait
}