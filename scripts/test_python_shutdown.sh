#!/bin/bash
# Test script to verify libcamera_preview.py handles shutdown signals correctly

echo "=== Testing Python Script Shutdown Handling ==="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if script exists
if [ ! -f "scripts/libcamera_preview.py" ]; then
    echo -e "${RED}❌ Error: scripts/libcamera_preview.py not found${NC}"
    exit 1
fi

echo "This test will:"
echo "  1. Start the Python preview script"
echo "  2. Wait 3 seconds"
echo "  3. Send SIGTERM (same signal Java sends)"
echo "  4. Verify GStreamer process is killed"
echo ""

# Cleanup first
echo "Cleaning up any existing processes..."
pkill -9 -f "libcamera_preview\.py" 2>/dev/null
pkill -9 -f "gst-launch.*webcam_preview" 2>/dev/null
sleep 1

# Check initial state
INITIAL_COUNT=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)
if [ "$INITIAL_COUNT" -gt 0 ]; then
    echo -e "${RED}❌ Warning: Found $INITIAL_COUNT existing process(es)${NC}"
    ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep
    echo ""
fi

echo "Starting Python preview script..."
python3 scripts/libcamera_preview.py /tmp/test_webcam_preview.jpg > /tmp/preview_stdout.log 2> /tmp/preview_stderr.log &
PYTHON_PID=$!

echo "Python script PID: $PYTHON_PID"
echo ""

# Wait for it to start
echo "Waiting for preview to initialize (3 seconds)..."
sleep 3

# Check if processes are running
echo ""
echo "Checking running processes..."
RUNNING_COUNT=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)

if [ "$RUNNING_COUNT" -eq 0 ]; then
    echo -e "${RED}❌ Preview didn't start (camera may not be available)${NC}"
    echo ""
    echo "=== STDOUT ==="
    cat /tmp/preview_stdout.log
    echo ""
    echo "=== STDERR ==="
    cat /tmp/preview_stderr.log
    rm -f /tmp/test_webcam_preview.jpg /tmp/preview_*.log
    exit 1
fi

echo -e "${GREEN}✅ Found $RUNNING_COUNT process(es) running:${NC}"
ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep
echo ""

# Get GStreamer PID from Python output
echo "=== Preview Debug Output ==="
cat /tmp/preview_stderr.log | grep "DEBUG:"
echo ""

# Now send SIGTERM (this is what Java does)
echo "Sending SIGTERM to Python script (PID $PYTHON_PID)..."
kill -TERM $PYTHON_PID

# Wait for graceful shutdown
echo "Waiting 2 seconds for graceful shutdown..."
sleep 2

# Check if processes are gone
AFTER_COUNT=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)

echo ""
echo "=== RESULTS ==="

if [ "$AFTER_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✅ SUCCESS: All processes terminated cleanly!${NC}"
    echo "  Before: $RUNNING_COUNT process(es)"
    echo "  After: $AFTER_COUNT process(es)"
    RESULT=0
else
    echo -e "${RED}❌ FAILURE: $AFTER_COUNT process(es) still running!${NC}"
    echo ""
    echo "Remaining processes:"
    ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep
    echo ""
    echo "These should have been killed by the Python script!"

    # Force cleanup
    echo ""
    echo "Force cleaning up..."
    pkill -9 -f "gst-launch.*webcam_preview" 2>/dev/null
    pkill -9 -f "libcamera_preview\.py" 2>/dev/null
    RESULT=1
fi

echo ""
echo "=== Python Script Output ==="
echo "STDOUT:"
cat /tmp/preview_stdout.log
echo ""
echo "STDERR:"
cat /tmp/preview_stderr.log

# Cleanup
echo ""
echo "Cleaning up test files..."
rm -f /tmp/test_webcam_preview.jpg /tmp/preview_*.log

echo ""
if [ $RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ Test PASSED - Python script handles shutdown correctly${NC}"
    echo ""
    echo "This means when Java calls process.destroy(), the Python script will:"
    echo "  1. Receive SIGTERM signal"
    echo "  2. Kill the GStreamer process group"
    echo "  3. Clean up temp files"
    echo "  4. Exit cleanly"
else
    echo -e "${RED}❌ Test FAILED - Python script does NOT handle shutdown correctly${NC}"
    echo ""
    echo "The Python script is not properly killing the GStreamer pipeline!"
    echo "Check the DEBUG output above for clues."
fi

exit $RESULT
