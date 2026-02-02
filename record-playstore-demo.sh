#!/bin/bash

# Record Demo Video for FOREGROUND_SERVICE_DATA_SYNC Permission
# Google Play Store Submission Requirement

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  CB Pro Proxy - Play Store Demo Video Recorder           ║${NC}"
echo -e "${BLUE}║  FOREGROUND_SERVICE_DATA_SYNC Permission Demonstration    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check ADB
if ! command -v adb &> /dev/null; then
    echo -e "${RED}✗ ADB not found!${NC}"
    echo -e "${YELLOW}Install Android SDK Platform Tools${NC}"
    exit 1
fi

# Check device
echo -e "${YELLOW}📱 Checking for connected device...${NC}"
DEVICE=$(adb devices | grep -w "device" | head -n 1 | awk '{print $1}')

if [ -z "$DEVICE" ]; then
    echo -e "${RED}✗ No device connected${NC}"
    echo -e "${YELLOW}Please connect your Android device via USB and enable USB debugging${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Device found: $DEVICE${NC}"
echo ""

# Get device info
DEVICE_MODEL=$(adb shell getprop ro.product.model)
ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
echo -e "${GREEN}  Model: $DEVICE_MODEL${NC}"
echo -e "${GREEN}  Android: $ANDROID_VERSION${NC}"
echo ""

# Prepare
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Preparation Steps${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${YELLOW}Please prepare your device:${NC}"
echo ""
echo -e "  1. ✓ CB Pro Proxy app is installed"
echo -e "  2. ✓ App is configured with a proxy server"
echo -e "  3. ✓ VPN is currently DISCONNECTED"
echo -e "  4. ✓ Screen brightness is high"
echo -e "  5. ✓ Notifications are visible"
echo -e "  6. ✓ Device is in portrait mode"
echo ""

read -p "Ready to start recording? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Recording cancelled${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Recording Instructions${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${GREEN}Follow these steps after recording starts:${NC}"
echo ""
echo -e "${YELLOW}Scene 1 (0-5s):${NC} Open CB Pro Proxy app"
echo -e "  → Show main screen"
echo ""
echo -e "${YELLOW}Scene 2 (5-15s):${NC} Show proxy configuration"
echo -e "  → Navigate to proxy settings"
echo -e "  → Display server/port/credentials"
echo ""
echo -e "${YELLOW}Scene 3 (15-30s):${NC} Connect VPN"
echo -e "  → Tap Connect button"
echo -e "  → ${RED}IMPORTANT:${NC} Accept VPN permission dialog"
echo -e "  → ${RED}IMPORTANT:${NC} Wait for notification to appear"
echo ""
echo -e "${YELLOW}Scene 4 (30-45s):${NC} Show data sync"
echo -e "  → Pull down notification shade"
echo -e "  → ${RED}HIGHLIGHT:${NC} Data counters updating (↑ ↓)"
echo -e "  → Keep notification visible for 5 seconds"
echo ""
echo -e "${YELLOW}Scene 5 (45-55s):${NC} Use internet"
echo -e "  → Open Chrome/Browser"
echo -e "  → Browse a website (google.com)"
echo -e "  → Show notification with increasing counters"
echo ""
echo -e "${YELLOW}Scene 6 (55-65s):${NC} Show connection stats"
echo -e "  → Return to CB Pro Proxy app"
echo -e "  → Display connection statistics"
echo ""
echo -e "${YELLOW}Scene 7 (65-70s):${NC} Disconnect"
echo -e "  → Tap Disconnect button"
echo -e "  → Show notification disappearing"
echo ""

read -p "Press Enter to start recording in 3 seconds... " 
echo ""

# Countdown
echo -e "${RED}3...${NC}"
sleep 1
echo -e "${RED}2...${NC}"
sleep 1
echo -e "${RED}1...${NC}"
sleep 1

# Start recording
OUTPUT_FILE="cbvpn-foreground-service-demo-$(date +%Y%m%d-%H%M%S).mp4"

echo -e "${GREEN}🎥 RECORDING STARTED${NC}"
echo -e "${YELLOW}Follow the scene instructions above${NC}"
echo ""
echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
echo -e "${RED}  Press ENTER to stop recording when you're done${NC}"
echo -e "${RED}═══════════════════════════════════════════════════════${NC}"
echo ""

# Record with high quality (max 3 minutes for safety)
adb shell screenrecord --verbose --bit-rate 8000000 --time-limit 180 /sdcard/$OUTPUT_FILE &
RECORD_PID=$!

# Timer in background
(
    for i in {1..180}; do
        echo -ne "\r${BLUE}Recording time: ${YELLOW}$i seconds${NC}   "
        sleep 1
    done
) &
TIMER_PID=$!

# Wait for user to press Enter
read -p ""

# Stop recording
echo ""
echo -e "${YELLOW}Stopping recording...${NC}"
kill $TIMER_PID 2>/dev/null || true
kill $RECORD_PID 2>/dev/null || true
wait $RECORD_PID 2>/dev/null || true
sleep 1

echo ""
echo ""
echo -e "${GREEN}🎬 Recording stopped${NC}"
echo ""

# Pull video
echo -e "${YELLOW}📥 Downloading video from device...${NC}"
adb pull /sdcard/$OUTPUT_FILE .

if [ -f "$OUTPUT_FILE" ]; then
    echo -e "${GREEN}✓ Video saved: $OUTPUT_FILE${NC}"
    
    # Get file size
    FILE_SIZE=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    echo -e "${GREEN}  Size: $FILE_SIZE${NC}"
    
    # Get duration
    if command -v ffprobe &> /dev/null; then
        DURATION=$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$OUTPUT_FILE" 2>/dev/null | awk '{printf "%.1f", $1}')
        echo -e "${GREEN}  Duration: ${DURATION}s${NC}"
    fi
    
    # Clean up device
    adb shell rm /sdcard/$OUTPUT_FILE
    
    echo ""
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  Next Steps${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${GREEN}1. Review the video:${NC}"
    echo -e "   open $OUTPUT_FILE"
    echo ""
    echo -e "${GREEN}2. Check these are visible:${NC}"
    echo -e "   ✓ Persistent notification with 'VPN Service'"
    echo -e "   ✓ Real-time data counters (↑ upload, ↓ download)"
    echo -e "   ✓ Connection duration timer"
    echo -e "   ✓ Clear start/stop actions"
    echo ""
    echo -e "${GREEN}3. If video is good:${NC}"
    echo -e "   → Upload to YouTube (unlisted)"
    echo -e "   → Or upload to Google Drive"
    echo -e "   → Share link in Play Store review response"
    echo ""
    echo -e "${GREEN}4. If you need to re-record:${NC}"
    echo -e "   → Run this script again"
    echo ""
    echo -e "${YELLOW}See docs/FOREGROUND_SERVICE_DATA_SYNC_EXPLANATION.md for full guide${NC}"
    echo ""
else
    echo -e "${RED}✗ Failed to download video${NC}"
    exit 1
fi
