package com.mediclinic.service;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryService;
import com.github.sarxos.webcam.WebcamException;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;

/**
 * Service for managing patient photos using webcam capture.
 * Photos are stored locally in the photos/ directory with naming pattern: patient_{id}.jpg
 */
public class PhotoService {

    private static final String PHOTOS_DIRECTORY = "photos";
    private static final String PHOTO_FORMAT = "jpg";
    private static final String PHOTO_EXTENSION = ".jpg";
    private static final String PHOTO_PREFIX = "patient_";

    private Webcam webcam;
    private boolean useNativeCapture = false;
    private String nativeWebcamDevice = "/dev/video0";
    private boolean isIntelIPU6 = false;

    static {
        // Configure webcam driver for better Linux compatibility
        try {
            // Set discovery timeout to avoid hanging
            WebcamDiscoveryService discovery = Webcam.getDiscoveryService();
            discovery.setEnabled(true);

            // Shorter timeout for Linux systems
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                System.out.println(
                    "Linux detected - will try native capture if Java webcam fails"
                );
            }
        } catch (Exception e) {
            System.err.println(
                "Warning: Could not configure webcam driver: " + e.getMessage()
            );
        }
    }

    public PhotoService() {
        // Ensure photos directory exists
        createPhotosDirectory();
    }

    /**
     * Creates the photos directory if it doesn't exist
     */
    private void createPhotosDirectory() {
        try {
            Path photosPath = Paths.get(PHOTOS_DIRECTORY);
            if (!Files.exists(photosPath)) {
                Files.createDirectories(photosPath);
                System.out.println(
                    "Created photos directory: " + photosPath.toAbsolutePath()
                );
            }
        } catch (IOException e) {
            System.err.println(
                "Error creating photos directory: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    /**
     * Opens the default webcam
     * @return true if webcam was opened successfully, false otherwise
     */
    public boolean openWebcam() {
        // First, try native Linux capture if on Linux
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            if (tryNativeLinuxWebcam()) {
                useNativeCapture = true;
                System.out.println(
                    "Using native Linux webcam capture via ffmpeg/v4l2"
                );
                return true;
            }
        }

        // Fall back to Java webcam library
        try {
            // Get webcams with timeout to avoid hanging on Linux
            java.util.List<Webcam> webcams;
            try {
                webcams = Webcam.getWebcams(1000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                System.err.println(
                    "Webcam discovery timed out or failed: " + e.getMessage()
                );
                return tryNativeLinuxWebcam();
            }

            if (webcams == null || webcams.isEmpty()) {
                System.err.println("No webcam detected on this system");
                return tryNativeLinuxWebcam();
            }

            webcam = webcams.get(0);
            System.out.println("Found webcam: " + webcam.getName());

            if (!webcam.isOpen()) {
                try {
                    // Set a reasonable view size before opening
                    java.awt.Dimension[] sizes = webcam.getViewSizes();
                    if (sizes != null && sizes.length > 0) {
                        // Use smallest size for better compatibility
                        webcam.setViewSize(sizes[0]);
                    }

                    // Try to open with non-blocking mode
                    webcam.open(true);

                    // Wait for webcam to initialize
                    int maxWait = 10; // 1 second total
                    for (int i = 0; i < maxWait && !webcam.isOpen(); i++) {
                        Thread.sleep(100);
                    }

                    if (!webcam.isOpen()) {
                        System.err.println(
                            "Webcam did not open within timeout"
                        );
                        return tryNativeLinuxWebcam();
                    }

                    System.out.println(
                        "Webcam opened successfully: " + webcam.getName()
                    );
                    useNativeCapture = false;
                    return true;
                } catch (WebcamException e) {
                    System.err.println("Webcam exception: " + e.getMessage());
                    System.err.println(
                        "This is a Linux v4l2 driver issue - trying native capture..."
                    );
                    return tryNativeLinuxWebcam();
                }
            }
            useNativeCapture = false;
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Webcam initialization interrupted");
            return false;
        } catch (Exception e) {
            System.err.println("Error opening webcam: " + e.getMessage());
            return tryNativeLinuxWebcam();
        }
    }

    /**
     * Try to use native Linux webcam capture as fallback
     * @return true if native capture is available
     */
    private boolean tryNativeLinuxWebcam() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("linux")) {
            return false;
        }

        // Detect Intel IPU6 camera (known to have driver issues)
        detectIntelIPU6();

        // Check if Python3 and gstreamer are available for libcamera capture
        if (
            isCommandAvailable("python3") &&
            isCommandAvailable("gst-launch-1.0")
        ) {
            // Check if capture script exists
            File scriptFile = new File("scripts/capture_photo_libcamera.py");
            if (scriptFile.exists()) {
                System.out.println(
                    "Native Linux webcam capture available via libcamera/pipewire"
                );
                if (isIntelIPU6) {
                    System.out.println(
                        "Intel IPU6 camera detected - using libcamera backend"
                    );
                }
                useNativeCapture = true;
                return true;
            } else {
                System.err.println(
                    "WARNING: Capture script not found: scripts/capture_photo_libcamera.py"
                );
            }
        }

        System.err.println(
            "Native capture not available. Missing requirements:"
        );
        if (!isCommandAvailable("python3")) {
            System.err.println(
                "  - python3 (install: sudo dnf install python3)"
            );
        }
        if (!isCommandAvailable("gst-launch-1.0")) {
            System.err.println(
                "  - gstreamer (install: sudo dnf install gstreamer1-tools gstreamer1-plugins-base)"
            );
        }
        System.err.println("Or use file upload instead of webcam.");
        return false;
    }

    /**
     * Detect if system has Intel IPU6 camera (known to have Linux driver issues)
     */
    private void detectIntelIPU6() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "v4l2-ctl",
                "--list-devices"
            );
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("ipu6")) {
                    isIntelIPU6 = true;
                    break;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // If we can't detect, assume it's not IPU6
            isIntelIPU6 = false;
        }
    }

    /**
     * Detect the first available webcam device
     */
    private void detectWebcamDevice() {
        // Try common video devices
        for (int i = 0; i < 32; i++) {
            String device = "/dev/video" + i;
            File deviceFile = new File(device);
            if (deviceFile.exists()) {
                nativeWebcamDevice = device;
                System.out.println("Using webcam device: " + device);
                return;
            }
        }
        nativeWebcamDevice = "/dev/video0"; // Default fallback
    }

    /**
     * Check if a command is available in PATH
     */
    private boolean isCommandAvailable(String command) {
        try {
            Process process = Runtime.getRuntime().exec(
                new String[] { "which", command }
            );
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Closes the webcam if it's open
     */
    public void closeWebcam() {
        if (webcam != null && webcam.isOpen()) {
            try {
                webcam.close();
                System.out.println("Webcam closed successfully");
            } catch (Exception e) {
                System.err.println("Error closing webcam: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Captures a photo from the webcam
     * @return BufferedImage of the captured photo, or null if capture failed
     */
    public BufferedImage capturePhoto() {
        if (useNativeCapture) {
            return capturePhotoNative();
        }

        if (webcam == null || !webcam.isOpen()) {
            System.err.println("Webcam is not open. Call openWebcam() first.");
            return null;
        }

        try {
            // Add retry logic for flaky webcam drivers
            BufferedImage image = null;
            int retries = 3;

            for (int i = 0; i < retries; i++) {
                try {
                    image = webcam.getImage();
                    if (image != null) {
                        System.out.println(
                            "Photo captured successfully (attempt " +
                                (i + 1) +
                                ")"
                        );
                        break;
                    }
                    Thread.sleep(100); // Brief pause between retries
                } catch (Exception e) {
                    if (i == retries - 1) {
                        throw e;
                    }
                    System.out.println(
                        "Capture attempt " + (i + 1) + " failed, retrying..."
                    );
                }
            }

            return image;
        } catch (Exception e) {
            System.err.println("Error capturing photo: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Capture photo using native Linux tools (libcamera via Python/GStreamer)
     * @return BufferedImage of the captured photo, or null if capture failed
     */
    private BufferedImage capturePhotoNative() {
        try {
            // Create temporary file for capture
            File tempFile = File.createTempFile("webcam_capture_", ".jpg");
            tempFile.deleteOnExit();

            // Use Python script with libcamera/pipewire/gstreamer
            // This works with Intel IPU6 cameras on Linux
            String scriptPath = "scripts/capture_photo_libcamera.py";
            File scriptFile = new File(scriptPath);

            if (!scriptFile.exists()) {
                System.err.println(
                    "ERROR: Camera capture script not found: " + scriptPath
                );
                System.err.println(
                    "Please ensure scripts/capture_photo_libcamera.py exists"
                );
                return null;
            }

            ProcessBuilder pb = new ProcessBuilder(
                "python3",
                scriptPath,
                tempFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (line.startsWith("SUCCESS")) {
                    System.out.println(line);
                } else if (line.startsWith("ERROR")) {
                    System.err.println(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && tempFile.exists() && tempFile.length() > 0) {
                BufferedImage image = ImageIO.read(tempFile);
                tempFile.delete();
                if (image != null) {
                    System.out.println(
                        "Photo captured successfully using libcamera (via pipewire/gstreamer)"
                    );
                    return image;
                }
            } else {
                System.err.println(
                    "Native capture failed with exit code: " + exitCode
                );
                if (output.length() > 0) {
                    System.err.println("Output:\n" + output.toString());
                }
            }

            tempFile.delete();
            return null;
        } catch (Exception e) {
            System.err.println(
                "Error in native photo capture: " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Saves a patient photo to disk
     * @param patientId the patient's ID
     * @param image the BufferedImage to save
     * @return true if save was successful, false otherwise
     */
    public boolean savePatientPhoto(Long patientId, BufferedImage image) {
        System.out.println(
            "DEBUG PhotoService.savePatientPhoto called - patientId: " +
                patientId +
                ", image: " +
                (image != null
                    ? image.getWidth() + "x" + image.getHeight()
                    : "NULL")
        );

        if (patientId == null) {
            System.err.println("ERROR: Patient ID cannot be null");
            return false;
        }

        if (image == null) {
            System.err.println("ERROR: Image cannot be null");
            return false;
        }

        try {
            File outputFile = getPatientPhotoFile(patientId);
            System.out.println(
                "DEBUG: Attempting to write image to: " +
                    outputFile.getAbsolutePath()
            );
            System.out.println(
                "DEBUG: Parent directory exists: " +
                    outputFile.getParentFile().exists()
            );
            System.out.println(
                "DEBUG: Parent directory writable: " +
                    outputFile.getParentFile().canWrite()
            );
            System.out.println(
                "DEBUG: Image type: " +
                    image.getType() +
                    " (TYPE_INT_ARGB=" +
                    BufferedImage.TYPE_INT_ARGB +
                    ", TYPE_INT_RGB=" +
                    BufferedImage.TYPE_INT_RGB +
                    ")"
            );

            // Convert image to RGB if it has alpha channel (JPEG doesn't support alpha)
            BufferedImage rgbImage = image;
            if (
                image.getType() == BufferedImage.TYPE_INT_ARGB ||
                image.getType() == BufferedImage.TYPE_INT_ARGB_PRE ||
                image.getType() == BufferedImage.TYPE_4BYTE_ABGR ||
                image.getType() == BufferedImage.TYPE_4BYTE_ABGR_PRE
            ) {
                System.out.println(
                    "DEBUG: Converting image from ARGB to RGB (removing alpha channel)"
                );

                rgbImage = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_INT_RGB
                );

                // Draw the original image onto the new RGB image
                // This removes the alpha channel
                java.awt.Graphics2D g = rgbImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();

                System.out.println(
                    "DEBUG: Conversion complete, new type: " +
                        rgbImage.getType()
                );
            }

            boolean writeSuccess = ImageIO.write(
                rgbImage,
                PHOTO_FORMAT,
                outputFile
            );

            if (writeSuccess) {
                System.out.println(
                    "SUCCESS: Photo saved to " +
                        outputFile.getAbsolutePath() +
                        " (size: " +
                        outputFile.length() +
                        " bytes)"
                );
                return true;
            } else {
                System.err.println(
                    "ERROR: ImageIO.write returned false - no suitable writer found for format: " +
                        PHOTO_FORMAT
                );
                return false;
            }
        } catch (IOException e) {
            System.err.println(
                "ERROR: IOException saving photo: " + e.getMessage()
            );
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Captures and saves a patient photo in one operation
     * @param patientId the patient's ID
     * @return the captured BufferedImage, or null if capture/save failed
     */
    public BufferedImage captureAndSavePatientPhoto(Long patientId) {
        BufferedImage image = capturePhoto();
        if (image != null) {
            if (savePatientPhoto(patientId, image)) {
                return image;
            }
        }
        return null;
    }

    /**
     * Loads a patient photo from disk
     * @param patientId the patient's ID
     * @return JavaFX Image if photo exists, null otherwise
     */
    public Image loadPatientPhoto(Long patientId) {
        if (patientId == null) {
            return null;
        }

        File photoFile = getPatientPhotoFile(patientId);

        if (!photoFile.exists()) {
            return null;
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(photoFile);
            if (bufferedImage != null) {
                System.out.println(
                    "Photo loaded successfully for patient ID: " + patientId
                );
                return SwingFXUtils.toFXImage(bufferedImage, null);
            }
        } catch (IOException e) {
            System.err.println(
                "Error loading photo for patient ID " +
                    patientId +
                    ": " +
                    e.getMessage()
            );
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Loads a photo from a file on disk
     * @param file the image file to load
     * @return BufferedImage if successful, null otherwise
     */
    public BufferedImage loadPhotoFromFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                System.out.println("Photo loaded from file: " + file.getName());
            }
            return image;
        } catch (IOException e) {
            System.err.println(
                "Error loading photo from file: " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a BufferedImage to a JavaFX Image
     * @param bufferedImage the BufferedImage to convert
     * @return JavaFX Image
     */
    public Image convertToFXImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    /**
     * Checks if a patient photo exists
     * @param patientId the patient's ID
     * @return true if photo exists, false otherwise
     */
    public boolean hasPatientPhoto(Long patientId) {
        if (patientId == null) {
            return false;
        }
        return getPatientPhotoFile(patientId).exists();
    }

    /**
     * Deletes a patient photo
     * @param patientId the patient's ID
     * @return true if deletion was successful or photo didn't exist, false if deletion failed
     */
    public boolean deletePatientPhoto(Long patientId) {
        if (patientId == null) {
            return false;
        }

        File photoFile = getPatientPhotoFile(patientId);

        if (!photoFile.exists()) {
            return true; // No photo to delete
        }

        try {
            boolean deleted = photoFile.delete();
            if (deleted) {
                System.out.println(
                    "Photo deleted successfully for patient ID: " + patientId
                );
            } else {
                System.err.println(
                    "Failed to delete photo for patient ID: " + patientId
                );
            }
            return deleted;
        } catch (Exception e) {
            System.err.println(
                "Error deleting photo for patient ID " +
                    patientId +
                    ": " +
                    e.getMessage()
            );
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the File object for a patient's photo
     * @param patientId the patient's ID
     * @return File object for the patient's photo
     */
    private File getPatientPhotoFile(Long patientId) {
        String filename = PHOTO_PREFIX + patientId + PHOTO_EXTENSION;
        return new File(PHOTOS_DIRECTORY, filename);
    }

    /**
     * Gets the full path to a patient's photo
     * @param patientId the patient's ID
     * @return absolute path to the photo file
     */
    public String getPatientPhotoPath(Long patientId) {
        return getPatientPhotoFile(patientId).getAbsolutePath();
    }

    /**
     * Checks if a webcam is available on the system
     * @return true if webcam is available, false otherwise
     */
    public boolean isWebcamAvailable() {
        // Check native Linux capture first
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            detectIntelIPU6();

            // Check if libcamera capture is available
            if (
                isCommandAvailable("python3") &&
                isCommandAvailable("gst-launch-1.0")
            ) {
                File scriptFile = new File(
                    "scripts/capture_photo_libcamera.py"
                );
                if (scriptFile.exists()) {
                    // Check if any video device exists
                    for (int i = 0; i < 32; i++) {
                        File device = new File("/dev/video" + i);
                        if (device.exists()) {
                            System.out.println(
                                "Found webcam device: /dev/video" + i
                            );
                            System.out.println(
                                "Native capture available via libcamera/pipewire"
                            );
                            if (isIntelIPU6) {
                                System.out.println(
                                    "Intel IPU6 camera detected - libcamera support enabled"
                                );
                            }
                            return true;
                        }
                    }
                }
            }
        }

        // Fall back to Java webcam library check
        try {
            // Quick check with timeout to avoid hanging
            java.util.List<Webcam> webcams = Webcam.getWebcams(
                500,
                TimeUnit.MILLISECONDS
            );
            if (webcams != null && !webcams.isEmpty()) {
                System.out.println("Found " + webcams.size() + " webcam(s)");
                for (Webcam cam : webcams) {
                    System.out.println("  - " + cam.getName());
                }
                return true;
            }
            System.out.println("No webcams found via Java library");
            return false;
        } catch (Exception e) {
            System.err.println(
                "Error checking webcam availability: " + e.getMessage()
            );
            System.err.println(
                "Note: Webcam may not be supported on this system (common on Linux with v4l2 issues)"
            );
            return false;
        }
    }

    /**
     * Gets the currently open webcam
     * @return Webcam instance or null if not open
     */
    public Webcam getWebcam() {
        return webcam;
    }

    /**
     * Checks if the webcam is currently open
     * @return true if webcam is open, false otherwise
     */
    public boolean isWebcamOpen() {
        if (useNativeCapture) {
            return true; // Native capture doesn't need "open" state
        }
        return webcam != null && webcam.isOpen();
    }

    /**
     * Checks if using native capture mode
     * @return true if using native Linux capture
     */
    public boolean isUsingNativeCapture() {
        return useNativeCapture;
    }
}
