#!/usr/bin/env python3
"""
High-performance libcamera preview using GStreamer + pipewire.
Provides 20-30 FPS continuous preview for Intel IPU6 cameras.
Uses the same backend as qcam (libcamera via pipewire).

Usage: python3 libcamera_preview.py <output_file.jpg>
"""

import os
import signal
import subprocess
import sys
import threading
import time


class LibcameraPreview:
    """Continuous preview using libcamera via pipewire"""

    def __init__(self, output_file):
        self.output_file = output_file
        self.running = False
        self.process = None
        self.process_pid = None
        self.frame_count = 0
        self.last_mtime = 0
        self.shutdown_requested = False

    def start(self):
        """Start continuous capture pipeline"""
        try:
            # Use GStreamer with pipewire source (same as qcam backend)
            # This pipeline continuously writes to the same file
            pipeline = [
                "gst-launch-1.0",
                "-q",
                "pipewiresrc",
                "!",
                "queue",
                "max-size-buffers=2",
                "leaky=downstream",
                "!",
                "videoconvert",
                "!",
                "video/x-raw,width=640,height=480",
                "!",
                "videorate",
                "!",
                "video/x-raw,framerate=20/1",
                "!",
                "jpegenc",
                "quality=85",
                "!",
                "multifilesink",
                f"location={self.output_file}",
                "max-files=1",
                "post-messages=true",
            ]

            # Start GStreamer in its own process group for proper cleanup
            self.process = subprocess.Popen(
                pipeline,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                preexec_fn=os.setsid,  # Create new process group
            )

            self.process_pid = self.process.pid
            self.running = True
            print("PREVIEW_STARTING", flush=True)
            print(
                f"DEBUG: GStreamer PID={self.process_pid}", file=sys.stderr, flush=True
            )

            # Wait for pipeline to start
            time.sleep(1.5)

            # Check if it's working
            if (
                os.path.exists(self.output_file)
                and os.path.getsize(self.output_file) > 0
            ):
                print("PREVIEW_READY", flush=True)
                return True
            else:
                print("ERROR: Pipeline failed to start", file=sys.stderr, flush=True)
                self.stop()
                return False

        except Exception as e:
            print(f"ERROR: {e}", file=sys.stderr, flush=True)
            self.stop()
            return False

    def monitor_frames(self):
        """Monitor file changes to detect new frames"""
        consecutive_no_change = 0

        while self.running and not self.shutdown_requested:
            try:
                if os.path.exists(self.output_file):
                    # Check file modification time instead of size
                    # (multifilesink updates the same file)
                    current_mtime = os.path.getmtime(self.output_file)

                    if current_mtime != self.last_mtime:
                        self.frame_count += 1
                        self.last_mtime = current_mtime
                        consecutive_no_change = 0

                        # Report progress every 20 frames
                        if self.frame_count % 20 == 0:
                            print(f"FRAME:{self.frame_count}", flush=True)
                    else:
                        consecutive_no_change += 1

                        # If no updates for 10 seconds, warn
                        if consecutive_no_change > 100:
                            print("WARNING: No new frames", file=sys.stderr, flush=True)
                            consecutive_no_change = 0

                # Check at 10 Hz
                time.sleep(0.1)

            except Exception as e:
                print(f"ERROR: Monitor error: {e}", file=sys.stderr, flush=True)
                break

        print(f"PREVIEW_STOPPED ({self.frame_count} frames)", flush=True)

    def stop(self):
        """Stop the pipeline gracefully and forcefully"""
        print("DEBUG: stop() called", file=sys.stderr, flush=True)
        self.running = False
        self.shutdown_requested = True

        if self.process and self.process_pid:
            try:
                pgid = os.getpgid(self.process_pid)
                print(
                    f"DEBUG: Killing process group {pgid}", file=sys.stderr, flush=True
                )

                # First try graceful shutdown (SIGTERM)
                try:
                    os.killpg(pgid, signal.SIGTERM)
                    print(
                        "DEBUG: Sent SIGTERM to process group",
                        file=sys.stderr,
                        flush=True,
                    )
                except ProcessLookupError:
                    print("DEBUG: Process already gone", file=sys.stderr, flush=True)
                    return

                # Wait briefly for graceful shutdown
                try:
                    self.process.wait(timeout=1)
                    print(
                        "DEBUG: Process exited gracefully", file=sys.stderr, flush=True
                    )
                except subprocess.TimeoutExpired:
                    # Force kill if still running
                    print(
                        "DEBUG: Process didn't exit, force killing...",
                        file=sys.stderr,
                        flush=True,
                    )
                    try:
                        os.killpg(pgid, signal.SIGKILL)
                        self.process.wait(timeout=1)
                        print(
                            "DEBUG: Process force killed", file=sys.stderr, flush=True
                        )
                    except Exception as e:
                        print(
                            f"DEBUG: Force kill error: {e}", file=sys.stderr, flush=True
                        )

            except Exception as e:
                print(f"WARNING: Stop error: {e}", file=sys.stderr, flush=True)

                # Last resort: try to kill by PID directly
                if self.process_pid:
                    try:
                        os.system(f"kill -9 {self.process_pid} 2>/dev/null")
                        os.system(f"pkill -9 -P {self.process_pid} 2>/dev/null")
                        print(
                            f"DEBUG: Killed PID {self.process_pid} via kill command",
                            file=sys.stderr,
                            flush=True,
                        )
                    except:
                        pass

        # Cleanup temp file
        try:
            if os.path.exists(self.output_file):
                os.remove(self.output_file)
                print(
                    f"DEBUG: Removed temp file {self.output_file}",
                    file=sys.stderr,
                    flush=True,
                )
        except Exception as e:
            print(f"DEBUG: Temp file cleanup error: {e}", file=sys.stderr, flush=True)


def capture_single_frame(output_file):
    """Capture a single high-quality frame (for final photo)"""
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
                "quality=95",
                "!",
                "filesink",
                f"location={output_file}",
            ],
            timeout=5,
            capture_output=True,
        )

        if result.returncode == 0 and os.path.exists(output_file):
            return True
        else:
            return False

    except Exception as e:
        print(f"ERROR: Single capture failed: {e}", file=sys.stderr, flush=True)
        return False


# Global preview instance for signal handler
preview_instance = None


def signal_handler(signum, frame):
    """Handle shutdown signals from Java or user"""
    global preview_instance

    signal_name = "SIGTERM" if signum == signal.SIGTERM else "SIGINT"
    print(f"\nDEBUG: Received {signal_name}", file=sys.stderr, flush=True)

    if preview_instance:
        print("DEBUG: Stopping preview...", file=sys.stderr, flush=True)
        preview_instance.stop()

    print("PREVIEW_INTERRUPTED", flush=True)
    sys.exit(0)


def main():
    global preview_instance

    if len(sys.argv) < 2:
        print("Usage: libcamera_preview.py <output_file.jpg>", file=sys.stderr)
        print("", file=sys.stderr)
        print(
            "This provides continuous preview using libcamera (like qcam)",
            file=sys.stderr,
        )
        sys.exit(1)

    output_file = sys.argv[1]

    # Check for --capture-only mode (single frame, no preview)
    if len(sys.argv) > 2 and sys.argv[2] == "--capture-only":
        print("Capturing single frame...", flush=True)
        if capture_single_frame(output_file):
            print(f"SUCCESS: Photo captured to {output_file}", flush=True)
            sys.exit(0)
        else:
            print("ERROR: Capture failed", file=sys.stderr, flush=True)
            sys.exit(1)

    # Ensure directory exists
    output_dir = os.path.dirname(output_file)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)

    # Create preview instance
    preview_instance = LibcameraPreview(output_file)

    # Handle signals gracefully (SIGTERM is what Java sends)
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    # Start continuous preview
    if preview_instance.start():
        try:
            # Monitor in main thread
            preview_instance.monitor_frames()
        except KeyboardInterrupt:
            print("\nDEBUG: KeyboardInterrupt", file=sys.stderr, flush=True)
        finally:
            preview_instance.stop()
    else:
        print("ERROR: Failed to start preview", file=sys.stderr, flush=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
