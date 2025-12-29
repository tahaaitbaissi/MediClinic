#!/bin/bash
echo "=============================================="
echo "  OpenCV Installation for MediClinic Webcam"
echo "=============================================="
echo ""
echo "Installing OpenCV for Python..."
echo ""

sudo dnf install python3-opencv -y

if python3 -c "import cv2" 2>/dev/null; then
    VERSION=$(python3 -c "import cv2; print(cv2.__version__)")
    echo ""
    echo "=============================================="
    echo "  ✓ OpenCV Successfully Installed!"
    echo "=============================================="
    echo ""
    echo "  Version: $VERSION"
    echo ""
    echo "Benefits:"
    echo "  ✓ Continuous video preview (30 FPS)"
    echo "  ✓ Smoother webcam experience"
    echo "  ✓ No double-capture issue"
    echo ""
    echo "Next: Build and run the application"
    echo "      Patient → Add → Take Photo"
    echo "      You'll see live video!"
    echo ""
else
    echo ""
    echo "Installation failed. Try: sudo dnf install python3-opencv"
    echo ""
    exit 1
fi
