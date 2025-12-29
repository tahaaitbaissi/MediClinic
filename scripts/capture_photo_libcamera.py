#!/usr/bin/env python3
"""
Cross-platform webcam photo capture script.
Supports Windows, macOS, and Linux with multiple fallback methods.

Usage:
  Single capture: python3 capture_photo_libcamera.py <output_file.jpg>
  Streaming mode: python3 capture_photo_libcamera.py --stream <output_dir>

Methods tried (in order):
- Linux: GStreamer + pipewire/v4l2, OpenCV, ffmpeg
- macOS: OpenCV, ffmpeg with AVFoundation
- Windows: OpenCV, ffmpeg with dshow, DirectShow
"""

import os
import platform
import signal
import subprocess
import sys
import time


def get_platform():
    """Detect the operating system"""
    system = platform.system().lower()
    if system == "linux":
        return "linux"
    elif system == "darwin":
        return "macos"
    elif system == "windows":
        return "windows"
    else:
        return "unknown"


def command_exists(command):
    """Check if a command exists in PATH"""
    try:
        if platform.system() == "Windows":
            subprocess.run(
                ["where", command],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                check=True,
            )
        else:
            subprocess.run(
                ["which", command],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                check=True,
            )
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False


def capture_with_opencv(output_file):
    """Capture using OpenCV (works on all platforms)"""
    try:
        import cv2

        print("Attempting capture with OpenCV...", file=sys.stderr)

        # Try to open the default camera
        camera = cv2.VideoCapture(0)

        if not camera.isOpened():
            print("ERROR: Could not open camera with OpenCV", file=sys.stderr)
            return False

        # Allow camera to warm up
        time.sleep(0.5)

        # Capture a few frames to ensure camera is ready
        for _ in range(5):
            camera.read()

        # Capture the actual photo
        ret, frame = camera.read()
        camera.release()

        if ret and frame is not None:
            # Save the image
            success = cv2.imwrite(output_file, frame, [cv2.IMWRITE_JPEG_QUALITY, 90])
            if success and os.path.exists(output_file):
                print(f"SUCCESS: Photo captured to {output_file}")
                return True
            else:
                print("ERROR: Failed to save image", file=sys.stderr)
                return False
        else:
            print("ERROR: Failed to capture frame", file=sys.stderr)
            return False

    except ImportError:
        print("INFO: OpenCV not available (pip install opencv-python)", file=sys.stderr)
        return False
    except Exception as e:
        print(f"ERROR: OpenCV capture failed: {e}", file=sys.stderr)
        return False


def stream_with_opencv(output_dir):
    """Stream video frames using OpenCV for continuous preview"""
    try:
        import cv2

        print("Starting OpenCV video stream...", file=sys.stderr)

        camera = cv2.VideoCapture(0)
        if not camera.isOpened():
            print("ERROR: Could not open camera", file=sys.stderr)
            return False

        # Set camera properties for better performance
        camera.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        camera.set(cv2.CAP_PROP_FPS, 15)

        print("STREAM_READY", file=sys.stdout)
        sys.stdout.flush()

        frame_count = 0
        running = True

        def signal_handler(signum, frame):
            nonlocal running
            running = False

        # Handle Ctrl+C gracefully
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)

        while running:
            ret, frame = camera.read()
            if not ret or frame is None:
                print("WARNING: Failed to capture frame", file=sys.stderr)
                time.sleep(0.1)
                continue

            # Save frame to output directory
            frame_file = os.path.join(output_dir, f"preview_frame.jpg")
            cv2.imwrite(frame_file, frame, [cv2.IMWRITE_JPEG_QUALITY, 85])

            # Print frame number for Java to know new frame is available
            frame_count += 1
            print(f"FRAME:{frame_count}", file=sys.stdout)
            sys.stdout.flush()

            # ~15 FPS
            time.sleep(0.066)

        camera.release()
        print("STREAM_STOPPED", file=sys.stdout)
        return True

    except ImportError:
        return False
    except Exception as e:
        print(f"ERROR: Stream failed: {e}", file=sys.stderr)
        return False


def capture_with_gstreamer_pipewire_linux(output_file):
    """Capture using GStreamer with pipewire (Linux with libcamera)"""
    if not command_exists("gst-launch-1.0"):
        return False

    try:
        print("Attempting capture with GStreamer + pipewire...", file=sys.stderr)

        cmd = [
            "gst-launch-1.0",
            "-q",
            "pipewiresrc",
            "do-timestamp=true",
            "!",
            "videoconvert",
            "!",
            "jpegenc",
            "quality=90",
            "!",
            "filesink",
            f"location={output_file}",
        ]

        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

        # Wait for camera to initialize and capture frame
        time.sleep(2)

        # Stop pipeline
        proc.terminate()
        proc.wait(timeout=2)

        if os.path.exists(output_file) and os.path.getsize(output_file) > 0:
            print(f"SUCCESS: Photo captured to {output_file}")
            return True
        else:
            return False

    except Exception as e:
        print(f"ERROR: GStreamer + pipewire failed: {e}", file=sys.stderr)
        return False


def capture_with_gstreamer_v4l2_linux(output_file):
    """Capture using GStreamer with v4l2 (Linux standard)"""
    if not command_exists("gst-launch-1.0"):
        return False

    try:
        print("Attempting capture with GStreamer + v4l2...", file=sys.stderr)

        cmd = [
            "gst-launch-1.0",
            "-q",
            "v4l2src",
            "num-buffers=1",
            "!",
            "videoconvert",
            "!",
            "jpegenc",
            "quality=90",
            "!",
            "filesink",
            f"location={output_file}",
        ]

        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
        )

        if (
            result.returncode == 0
            and os.path.exists(output_file)
            and os.path.getsize(output_file) > 0
        ):
            print(f"SUCCESS: Photo captured to {output_file}")
            return True
        else:
            return False

    except Exception as e:
        print(f"ERROR: GStreamer + v4l2 failed: {e}", file=sys.stderr)
        return False


def capture_with_ffmpeg_linux(output_file):
    """Capture using ffmpeg with v4l2 (Linux)"""
    if not command_exists("ffmpeg"):
        return False

    try:
        print("Attempting capture with ffmpeg + v4l2...", file=sys.stderr)

        cmd = [
            "ffmpeg",
            "-f",
            "v4l2",
            "-i",
            "/dev/video0",
            "-frames:v",
            "1",
            "-q:v",
            "2",
            "-y",
            output_file,
        ]

        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
        )

        if result.returncode == 0 and os.path.exists(output_file):
            print(f"SUCCESS: Photo captured to {output_file}")
            return True
        else:
            return False

    except Exception as e:
        print(f"ERROR: ffmpeg + v4l2 failed: {e}", file=sys.stderr)
        return False


def capture_with_ffmpeg_macos(output_file):
    """Capture using ffmpeg with AVFoundation (macOS)"""
    if not command_exists("ffmpeg"):
        return False

    try:
        print("Attempting capture with ffmpeg + AVFoundation...", file=sys.stderr)

        # First, try to list devices to find the default camera
        list_cmd = ["ffmpeg", "-f", "avfoundation", "-list_devices", "true", "-i", ""]
        list_result = subprocess.run(
            list_cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=3,
        )

        # Use default video device (usually index 0)
        cmd = [
            "ffmpeg",
            "-f",
            "avfoundation",
            "-framerate",
            "30",
            "-i",
            "0",  # Default video device
            "-frames:v",
            "1",
            "-q:v",
            "2",
            "-y",
            output_file,
        ]

        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=10,
        )

        if result.returncode == 0 and os.path.exists(output_file):
            print(f"SUCCESS: Photo captured to {output_file}")
            return True
        else:
            return False

    except Exception as e:
        print(f"ERROR: ffmpeg + AVFoundation failed: {e}", file=sys.stderr)
        return False


def capture_with_ffmpeg_windows(output_file):
    """Capture using ffmpeg with DirectShow (Windows)"""
    if not command_exists("ffmpeg"):
        return False

    try:
        print("Attempting capture with ffmpeg + DirectShow...", file=sys.stderr)

        # Try to find the video device name
        list_cmd = ["ffmpeg", "-list_devices", "true", "-f", "dshow", "-i", "dummy"]
        list_result = subprocess.run(
            list_cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=3,
        )

        # Use generic video device name
        cmd = [
            "ffmpeg",
            "-f",
            "dshow",
            "-i",
            "video=Integrated Camera",  # Common name
            "-frames:v",
            "1",
            "-q:v",
            "2",
            "-y",
            output_file,
        ]

        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=10,
        )

        if result.returncode == 0 and os.path.exists(output_file):
            print(f"SUCCESS: Photo captured to {output_file}")
            return True

        # If that didn't work, try with generic name
        cmd[4] = "video=0"
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=10,
        )

        if result.returncode == 0 and os.path.exists(output_file):
            print(f"SUCCESS: Photo captured to {output_file}")
            return True

        return False

    except Exception as e:
        print(f"ERROR: ffmpeg + DirectShow failed: {e}", file=sys.stderr)
        return False


def capture_photo_linux(output_file):
    """Try all Linux capture methods"""
    methods = [
        ("OpenCV", lambda: capture_with_opencv(output_file)),
        (
            "GStreamer + pipewire",
            lambda: capture_with_gstreamer_pipewire_linux(output_file),
        ),
        ("GStreamer + v4l2", lambda: capture_with_gstreamer_v4l2_linux(output_file)),
        ("ffmpeg + v4l2", lambda: capture_with_ffmpeg_linux(output_file)),
    ]

    for method_name, method_func in methods:
        try:
            if method_func():
                return True
        except Exception as e:
            print(f"ERROR: {method_name} failed: {e}", file=sys.stderr)
            continue

    return False


def capture_photo_macos(output_file):
    """Try all macOS capture methods"""
    methods = [
        ("OpenCV", lambda: capture_with_opencv(output_file)),
        ("ffmpeg + AVFoundation", lambda: capture_with_ffmpeg_macos(output_file)),
    ]

    for method_name, method_func in methods:
        try:
            if method_func():
                return True
        except Exception as e:
            print(f"ERROR: {method_name} failed: {e}", file=sys.stderr)
            continue

    return False


def capture_photo_windows(output_file):
    """Try all Windows capture methods"""
    methods = [
        ("OpenCV", lambda: capture_with_opencv(output_file)),
        ("ffmpeg + DirectShow", lambda: capture_with_ffmpeg_windows(output_file)),
    ]

    for method_name, method_func in methods:
        try:
            if method_func():
                return True
        except Exception as e:
            print(f"ERROR: {method_name} failed: {e}", file=sys.stderr)
            continue

    return False


def main():
    # Check for streaming mode
    if len(sys.argv) >= 3 and sys.argv[1] == "--stream":
        output_dir = sys.argv[2]

        # Ensure output directory exists
        if not os.path.exists(output_dir):
            os.makedirs(output_dir, exist_ok=True)

        # Try streaming with OpenCV (most reliable for streaming)
        if stream_with_opencv(output_dir):
            sys.exit(0)
        else:
            print("ERROR: Video streaming not available", file=sys.stderr)
            print("Install OpenCV: pip install opencv-python", file=sys.stderr)
            sys.exit(1)

    # Single capture mode
    if len(sys.argv) < 2:
        print(
            "Usage:",
            file=sys.stderr,
        )
        print(
            "  Single capture: python3 capture_photo_libcamera.py <output_file.jpg>",
            file=sys.stderr,
        )
        print(
            "  Streaming mode: python3 capture_photo_libcamera.py --stream <output_dir>",
            file=sys.stderr,
        )
        sys.exit(1)

    output_file = sys.argv[1]

    # Ensure output directory exists
    output_dir = os.path.dirname(output_file)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)

    # Detect platform and use appropriate capture method
    current_platform = get_platform()
    print(f"Detected platform: {current_platform}", file=sys.stderr)

    success = False

    if current_platform == "linux":
        success = capture_photo_linux(output_file)
    elif current_platform == "macos":
        success = capture_photo_macos(output_file)
    elif current_platform == "windows":
        success = capture_photo_windows(output_file)
    else:
        print(f"ERROR: Unsupported platform: {current_platform}", file=sys.stderr)

    if success:
        sys.exit(0)
    else:
        print("", file=sys.stderr)
        print("=" * 60, file=sys.stderr)
        print("ERROR: Camera capture not available on this system", file=sys.stderr)
        print("=" * 60, file=sys.stderr)
        print("", file=sys.stderr)
        print("SOLUTION: Use 'Upload Photo' instead of 'Take Photo'", file=sys.stderr)
        print("", file=sys.stderr)
        print("To enable webcam capture, install one of these:", file=sys.stderr)
        print("  - OpenCV: pip install opencv-python", file=sys.stderr)
        print("  - ffmpeg: https://ffmpeg.org/download.html", file=sys.stderr)

        if current_platform == "linux":
            print("  - GStreamer: sudo dnf install gstreamer1-tools", file=sys.stderr)

        print("=" * 60, file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
