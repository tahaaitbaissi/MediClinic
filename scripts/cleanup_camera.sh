#!/bin/bash
# Quick cleanup script for orphaned camera processes
# Run this if the camera preview doesn't close properly

echo "=== Camera Process Cleanup ==="
echo ""

# Find and display processes
echo "Searching for camera-related processes..."
PROCESSES=$(ps aux | grep -E "(gst-launch|libcamera_preview\.py|qcam)" | grep -v grep)

if [ -z "$PROCESSES" ]; then
    echo "✅ No camera processes found - system is clean"
    exit 0
fi

echo "Found the following processes:"
echo "$PROCESSES"
echo ""

# Ask for confirmation if running interactively
if [ -t 0 ]; then
    read -p "Kill these processes? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cancelled."
        exit 0
    fi
fi

echo ""
echo "Stopping processes gracefully..."

# Try graceful shutdown first (SIGTERM)
pkill -f "gst-launch.*webcam_preview" 2>/dev/null
pkill -f "libcamera_preview\.py" 2>/dev/null
pkill qcam 2>/dev/null

# Wait a bit
sleep 2

# Check if any are still running
REMAINING=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)

if [ "$REMAINING" -gt 0 ]; then
    echo "Some processes didn't stop, forcing..."
    pkill -9 -f "gst-launch.*webcam_preview" 2>/dev/null
    pkill -9 -f "libcamera_preview\.py" 2>/dev/null
    sleep 1
fi

# Cleanup temp files
if [ -f "/tmp/webcam_preview.jpg" ]; then
    echo "Removing temp file: /tmp/webcam_preview.jpg"
    rm -f /tmp/webcam_preview.jpg
fi

# Final check
FINAL=$(ps aux | grep -E "(gst-launch.*webcam_preview|libcamera_preview\.py)" | grep -v grep | wc -l)

if [ "$FINAL" -eq 0 ]; then
    echo ""
    echo "✅ All camera processes stopped successfully"
    echo ""
else
    echo ""
    echo "⚠️  Some processes may still be running:"
    ps aux | grep -E "(gst-launch|libcamera_preview)" | grep -v grep
    echo ""
    echo "Try running with sudo if processes persist:"
    echo "  sudo $0"
fi

# Show camera device status
echo ""
echo "Camera device status:"
if command -v v4l2-ctl &> /dev/null; then
    v4l2-ctl --list-devices 2>/dev/null || echo "  (v4l2-ctl available but no devices found)"
else
    echo "  (v4l2-ctl not available)"
fi

exit 0
