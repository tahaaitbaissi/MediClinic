#!/usr/bin/env python3
"""
Continuous webcam preview using GStreamer + pipewire.
Captures frames continuously and saves to a single file.
Works with Intel IPU6 cameras on Linux.
"""

import os
import subprocess
import sys
import time


def capture_frame(output_file):
    """Capture a single frame using GStreamer"""
    try:
        result = subprocess.run(
            [
                "gst-launch-1.0",
                "-q",
                "pipewiresrc",
                "num-buffers=1",
                "!",
                "videoconvert",
                "!",
                "video/x-raw,width=640,height=480",
                "!",
                "jpegenc",
                "quality=85",
                "!",
                "filesink",
                f"location={output_file}",
            ],
            timeout=3,
            capture_output=True,
        )

        return result.returncode == 0
    except subprocess.TimeoutExpired:
        return False
    except Exception as e:
        print(f"ERROR:{e}", file=sys.stderr, flush=True)
        return False


def main():
    if len(sys.argv) < 2:
        print("Usage: gstreamer_preview.py <output_file.jpg>", file=sys.stderr)
        sys.exit(1)

    output_file = sys.argv[1]

    # Ensure directory exists
    output_dir = os.path.dirname(output_file)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)

    print("PREVIEW_STARTING", flush=True)

    frame_count = 0
    error_count = 0
    max_errors = 5

    while True:
        try:
            success = capture_frame(output_file)

            if success:
                frame_count += 1
                error_count = 0  # Reset error count on success

                # Print status every 10 frames
                if frame_count % 10 == 0:
                    print(f"FRAME:{frame_count}", flush=True)
            else:
                error_count += 1
                if error_count >= max_errors:
                    print(
                        f"ERROR:Too many capture failures ({max_errors})",
                        file=sys.stderr,
                        flush=True,
                    )
                    break

            # ~10 FPS (good balance for Intel IPU6)
            time.sleep(0.1)

        except KeyboardInterrupt:
            print("PREVIEW_INTERRUPTED", flush=True)
            break
        except Exception as e:
            print(f"ERROR:{e}", file=sys.stderr, flush=True)
            error_count += 1
            if error_count >= max_errors:
                break
            time.sleep(0.5)

    print("PREVIEW_STOPPED", flush=True)
    print(f"Total frames captured: {frame_count}", flush=True)


if __name__ == "__main__":
    main()
