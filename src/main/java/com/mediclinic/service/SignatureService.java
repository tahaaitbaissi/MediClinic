package com.mediclinic.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;

/**
 * Service for managing doctor electronic signatures.
 * Handles signature storage, retrieval, and validation.
 */
public class SignatureService {

    private static final String SIGNATURES_DIRECTORY = "signatures";
    private static final String SIGNATURE_FORMAT = "png";
    private static final String SIGNATURE_EXTENSION = ".png";
    private static final String SIGNATURE_PREFIX = "medecin_";

    static {
        // Ensure signatures directory exists on service initialization
        try {
            Path signaturesPath = Paths.get(SIGNATURES_DIRECTORY);
            if (!Files.exists(signaturesPath)) {
                Files.createDirectories(signaturesPath);
                System.out.println(
                    "Created signatures directory: " +
                        signaturesPath.toAbsolutePath()
                );
            }
        } catch (IOException e) {
            System.err.println(
                "Error creating signatures directory: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    public SignatureService() {
        // Ensure directory exists
        createSignaturesDirectory();
    }

    /**
     * Creates the signatures directory if it doesn't exist
     */
    private void createSignaturesDirectory() {
        try {
            Path signaturesPath = Paths.get(SIGNATURES_DIRECTORY);
            if (!Files.exists(signaturesPath)) {
                Files.createDirectories(signaturesPath);
                System.out.println(
                    "Created signatures directory: " +
                        signaturesPath.toAbsolutePath()
                );
            }
        } catch (IOException e) {
            System.err.println(
                "Error creating signatures directory: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    /**
     * Saves a doctor's signature image
     * @param medecinId the doctor's ID
     * @param signature the signature image (WritableImage from Canvas)
     * @return true if save was successful, false otherwise
     */
    public boolean saveSignature(Long medecinId, WritableImage signature) {
        if (medecinId == null) {
            System.err.println("ERROR: Medecin ID cannot be null");
            return false;
        }

        if (signature == null) {
            System.err.println("ERROR: Signature image cannot be null");
            return false;
        }

        try {
            File outputFile = getSignatureFile(medecinId);
            System.out.println(
                "DEBUG: Saving signature to: " + outputFile.getAbsolutePath()
            );

            // Convert JavaFX WritableImage to BufferedImage
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(
                signature,
                null
            );

            // Check if image needs alpha removal (though PNG supports alpha)
            // For consistency with photo service, we could convert to RGB
            // But PNG supports transparency, which is good for signatures
            boolean writeSuccess = ImageIO.write(
                bufferedImage,
                SIGNATURE_FORMAT,
                outputFile
            );

            if (writeSuccess) {
                System.out.println(
                    "SUCCESS: Signature saved to " +
                        outputFile.getAbsolutePath() +
                        " (size: " +
                        outputFile.length() +
                        " bytes)"
                );
                return true;
            } else {
                System.err.println(
                    "ERROR: ImageIO.write returned false - no suitable writer found for format: " +
                        SIGNATURE_FORMAT
                );
                return false;
            }
        } catch (IOException e) {
            System.err.println(
                "ERROR: IOException saving signature: " + e.getMessage()
            );
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads a doctor's signature as a JavaFX Image
     * @param medecinId the doctor's ID
     * @return JavaFX Image of the signature, or null if not found
     */
    public Image loadSignature(Long medecinId) {
        if (medecinId == null) {
            System.err.println("ERROR: Medecin ID cannot be null");
            return null;
        }

        File signatureFile = getSignatureFile(medecinId);

        if (!signatureFile.exists()) {
            System.out.println(
                "INFO: No signature found for medecin " + medecinId
            );
            return null;
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(signatureFile);
            if (bufferedImage == null) {
                System.err.println("ERROR: Failed to read signature image");
                return null;
            }

            Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
            System.out.println(
                "SUCCESS: Loaded signature for medecin " + medecinId
            );
            return fxImage;
        } catch (IOException e) {
            System.err.println(
                "ERROR: Failed to load signature: " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads a doctor's signature as a BufferedImage (for PDF generation)
     * @param medecinId the doctor's ID
     * @return BufferedImage of the signature, or null if not found
     */
    public BufferedImage loadSignatureAsBufferedImage(Long medecinId) {
        if (medecinId == null) {
            System.err.println("ERROR: Medecin ID cannot be null");
            return null;
        }

        File signatureFile = getSignatureFile(medecinId);

        if (!signatureFile.exists()) {
            System.out.println(
                "INFO: No signature found for medecin " + medecinId
            );
            return null;
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(signatureFile);
            if (bufferedImage == null) {
                System.err.println("ERROR: Failed to read signature image");
                return null;
            }

            System.out.println(
                "SUCCESS: Loaded signature as BufferedImage for medecin " +
                    medecinId
            );
            return bufferedImage;
        } catch (IOException e) {
            System.err.println(
                "ERROR: Failed to load signature: " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the file path for a doctor's signature
     * @param medecinId the doctor's ID
     * @return absolute path to the signature file
     */
    public String getSignatureFilePath(Long medecinId) {
        return getSignatureFile(medecinId).getAbsolutePath();
    }

    /**
     * Checks if a doctor has a saved signature
     * @param medecinId the doctor's ID
     * @return true if signature exists, false otherwise
     */
    public boolean hasSignature(Long medecinId) {
        if (medecinId == null) {
            return false;
        }

        File signatureFile = getSignatureFile(medecinId);
        boolean exists = signatureFile.exists() && signatureFile.length() > 0;

        if (exists) {
            System.out.println(
                "INFO: Signature exists for medecin " +
                    medecinId +
                    " (" +
                    signatureFile.length() +
                    " bytes)"
            );
        }

        return exists;
    }

    /**
     * Deletes a doctor's signature
     * @param medecinId the doctor's ID
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteSignature(Long medecinId) {
        if (medecinId == null) {
            System.err.println("ERROR: Medecin ID cannot be null");
            return false;
        }

        File signatureFile = getSignatureFile(medecinId);

        if (!signatureFile.exists()) {
            System.out.println(
                "INFO: No signature to delete for medecin " + medecinId
            );
            return true; // Nothing to delete is considered success
        }

        try {
            boolean deleted = signatureFile.delete();
            if (deleted) {
                System.out.println(
                    "SUCCESS: Deleted signature for medecin " + medecinId
                );
            } else {
                System.err.println(
                    "ERROR: Failed to delete signature file: " +
                        signatureFile.getAbsolutePath()
                );
            }
            return deleted;
        } catch (Exception e) {
            System.err.println(
                "ERROR: Exception deleting signature: " + e.getMessage()
            );
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Validates a signature image (checks dimensions, size, etc.)
     * @param signature the signature image to validate
     * @return true if valid, false otherwise
     */
    public boolean validateSignature(WritableImage signature) {
        if (signature == null) {
            System.err.println("ERROR: Signature image is null");
            return false;
        }

        int width = (int) signature.getWidth();
        int height = (int) signature.getHeight();

        // Check minimum dimensions (signature should be at least 50x20 pixels)
        if (width < 50 || height < 20) {
            System.err.println(
                "ERROR: Signature too small (" + width + "x" + height + ")"
            );
            return false;
        }

        // Check maximum dimensions (reasonable limit)
        if (width > 2000 || height > 1000) {
            System.err.println(
                "ERROR: Signature too large (" + width + "x" + height + ")"
            );
            return false;
        }

        System.out.println(
            "INFO: Signature validated (" + width + "x" + height + ")"
        );
        return true;
    }

    /**
     * Gets the File object for a doctor's signature
     * @param medecinId the doctor's ID
     * @return File object for the signature
     */
    private File getSignatureFile(Long medecinId) {
        String filename =
            SIGNATURE_PREFIX + medecinId + SIGNATURE_EXTENSION;
        return new File(SIGNATURES_DIRECTORY, filename);
    }

    /**
     * Checks if the signature is empty (all pixels are transparent/white)
     * This is a basic check - a more sophisticated version would analyze pixels
     * @param signature the signature image
     * @return true if signature appears empty, false otherwise
     */
    public boolean isSignatureEmpty(WritableImage signature) {
        if (signature == null) {
            return true;
        }

        // For now, just check dimensions
        // A real implementation would check pixel data
        int width = (int) signature.getWidth();
        int height = (int) signature.getHeight();

        return width == 0 || height == 0;
    }

    /**
     * Gets statistics about stored signatures
     * @return String with signature statistics
     */
    public String getSignatureStats() {
        File signaturesDir = new File(SIGNATURES_DIRECTORY);

        if (!signaturesDir.exists() || !signaturesDir.isDirectory()) {
            return "Signatures directory not found";
        }

        File[] signatureFiles = signaturesDir.listFiles(
            (dir, name) -> name.endsWith(SIGNATURE_EXTENSION)
        );

        if (signatureFiles == null || signatureFiles.length == 0) {
            return "No signatures stored";
        }

        long totalSize = 0;
        for (File file : signatureFiles) {
            totalSize += file.length();
        }

        return String.format(
            "%d signature(s) stored, total size: %d KB",
            signatureFiles.length,
            totalSize / 1024
        );
    }
}
