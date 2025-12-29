#!/bin/bash
# Verification script for webcam capture fix
# Tests that GStreamer processes are properly stopped when capture button is clicked

echo "=== Webcam Capture Fix Verification ==="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check for orphaned processes
check_processes() {
    echo "Checking for GStreamer/libcamera processes..."

    GST_COUNT=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)

    if [ "$GST_COUNT" -gt 0 ]; then
        echo -e "${RED}❌ Found $GST_COUNT orphaned process(es):${NC}"
        ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep
        echo ""
        echo "These processes should have been killed when you clicked Capture!"
        return 1
    else
        echo -e "${GREEN}✅ No orphaned processes found${NC}"
        return 0
    fi
}

# Function to test camera availability
test_camera() {
    echo ""
    echo "Testing camera availability..."

    if command -v qcam &> /dev/null; then
        echo -e "${GREEN}✅ qcam available${NC}"
    else
        echo -e "${YELLOW}⚠️  qcam not found (OK if not on Linux)${NC}"
    fi

    if command -v gst-launch-1.0 &> /dev/null; then
        echo -e "${GREEN}✅ GStreamer available${NC}"
    else
        echo -e "${YELLOW}⚠️  GStreamer not found${NC}"
    fi

    if [ -f "scripts/libcamera_preview.py" ]; then
        echo -e "${GREEN}✅ libcamera_preview.py present${NC}"
    else
        echo -e "${RED}❌ libcamera_preview.py missing${NC}"
    fi
}

# Function to kill orphaned processes
cleanup_orphans() {
    echo ""
    echo "Cleaning up orphaned processes..."

    # First count them
    COUNT_BEFORE=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)

    if [ "$COUNT_BEFORE" -eq 0 ]; then
        echo -e "${GREEN}✅ No processes to clean up${NC}"
        return
    fi

    echo "Found $COUNT_BEFORE process(es) to clean..."

    # Try graceful first
    pkill -TERM -f "gst-launch.*webcam_preview" 2>/dev/null
    pkill -TERM -f "libcamera_preview\.py" 2>/dev/null

    # Give them time to exit
    sleep 2

    # Force kill if still running
    pkill -9 -f "gst-launch.*webcam_preview" 2>/dev/null
    pkill -9 -f "libcamera_preview\.py" 2>/dev/null

    sleep 1

    # Verify cleanup
    COUNT_AFTER=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)

    if [ "$COUNT_AFTER" -eq 0 ]; then
        echo -e "${GREEN}✅ Cleanup complete - all $COUNT_BEFORE process(es) terminated${NC}"
    else
        echo -e "${RED}❌ Warning: $COUNT_AFTER process(es) still running${NC}"
        ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep
    fi
}

# Function to monitor processes during test
monitor_during_capture() {
    echo ""
    echo "=== Manual Test Instructions ==="
    echo ""
    echo "1. Start the MediClinic application"
    echo "2. Go to Patients → Add/Edit Patient"
    echo "3. Click 'Take Photo' button"
    echo "4. Wait for preview to appear (you should see live feed)"
    echo ""
    echo "MONITORING: Open another terminal and run:"
    echo "  watch -n 0.5 'ps aux | grep -E \"(gst-launch|libcamera_preview)\" | grep -v grep | wc -l'"
    echo ""
    echo "5. Click '📷 Capturer' button"
    echo ""
    echo "EXPECTED RESULTS:"
    echo "  ✅ Preview stops IMMEDIATELY (within 100ms)"
    echo "  ✅ Process count in watch terminal goes from 2 → 0"
    echo "  ✅ Dialog closes"
    echo "  ✅ Photo saved to photos/patient_X.jpg"
    echo "  ✅ Console shows: 'GStreamer process stopped on capture'"
    echo "  ✅ No 'Link has been severed' errors"
    echo ""
    echo "FAILURE SIGNS:"
    echo "  ❌ Preview keeps running after clicking Capture"
    echo "  ❌ Process count stays at 2 in watch terminal"
    echo "  ❌ Photo not captured/saved"
    echo ""
    echo "6. After capture, run: ./scripts/verify_capture_fix.sh --check"
    echo "   Should show: ✅ No orphaned processes found"
    echo ""
    echo "7. Press Ctrl+C when done testing"
    echo ""
}

# Function to check temp files
check_temp_files() {
    echo ""
    echo "Checking temporary files..."

    if [ -f "/tmp/webcam_preview.jpg" ]; then
        FILE_SIZE=$(stat -f%z "/tmp/webcam_preview.jpg" 2>/dev/null || stat -c%s "/tmp/webcam_preview.jpg" 2>/dev/null)
        FILE_AGE=$(stat -f%m "/tmp/webcam_preview.jpg" 2>/dev/null || stat -c%Y "/tmp/webcam_preview.jpg" 2>/dev/null)
        CURRENT_TIME=$(date +%s)
        AGE_SECONDS=$((CURRENT_TIME - FILE_AGE))

        echo "  File: /tmp/webcam_preview.jpg"
        echo "  Size: $FILE_SIZE bytes"
        echo "  Age: $AGE_SECONDS seconds"

        if [ "$AGE_SECONDS" -lt 10 ]; then
            echo -e "${YELLOW}⚠️  Recent temp file (may indicate active capture)${NC}"
        else
            echo -e "${GREEN}✅ Temp file is old (inactive)${NC}"
        fi
    else
        echo -e "${GREEN}✅ No temp preview file${NC}"
    fi
}

# Main test flow
main() {
    echo "Starting verification at $(date)"
    echo ""

    # Initial cleanup
    cleanup_orphans

    # Check environment
    test_camera
    check_temp_files

    echo ""
    echo "=== Pre-test Check ==="
    check_processes

    # Show monitoring instructions
    monitor_during_capture

    # Wait for user to test
    echo "Waiting for manual test (Ctrl+C to finish)..."
    echo ""

    while true; do
        sleep 1

        # Periodic check
        GST_COUNT=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)
        QCAM_COUNT=$(ps aux | grep -E "qcam" | grep -v grep | wc -l)

        if [ "$GST_COUNT" -gt 0 ] || [ "$QCAM_COUNT" -gt 0 ]; then
            TIMESTAMP=$(date +%H:%M:%S)
            if [ "$GST_COUNT" -gt 0 ]; then
                echo "$TIMESTAMP - GStreamer/libcamera: $GST_COUNT process(es)"
            fi
            if [ "$QCAM_COUNT" -gt 0 ]; then
                echo "$TIMESTAMP - qcam: $QCAM_COUNT process(es)"
            fi
        fi
    done
}

# Handle Ctrl+C
trap 'echo ""; echo ""; echo "=== Final Check ==="; check_processes; check_temp_files; echo ""; echo "Test complete!"; exit 0' INT

# Show usage
if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --cleanup    Kill all orphaned GStreamer/libcamera processes"
    echo "  --check      Check for orphaned processes only"
    echo "  --help       Show this help message"
    echo ""
    echo "No options:   Run full verification (default)"
    exit 0
fi

# Handle flags
if [ "$1" == "--cleanup" ]; then
    cleanup_orphans
    check_processes
    exit 0
fi

if [ "$1" == "--check" ]; then
    check_processes
    check_temp_files
    exit $?
fi

# Run main test
main
