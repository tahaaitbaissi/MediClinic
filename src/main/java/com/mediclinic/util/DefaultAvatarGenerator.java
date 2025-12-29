package com.mediclinic.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Utility class to generate default avatar images for patients
 */
public class DefaultAvatarGenerator {

    private static final int DEFAULT_SIZE = 150;
    private static final Color BACKGROUND_COLOR = Color.rgb(230, 230, 250);
    private static final Color ICON_COLOR = Color.rgb(100, 100, 120);

    /**
     * Generates a default avatar image with a person icon
     * @param size the size of the image (width and height)
     * @return JavaFX Image of the default avatar
     */
    public static Image generateDefaultAvatar(int size) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();

        // Fill background
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                writer.setColor(x, y, BACKGROUND_COLOR);
            }
        }

        // Draw a simple person icon (circle for head + body)
        int centerX = size / 2;

        // Draw head (circle)
        int headRadius = size / 6;
        int headY = size / 3;
        drawCircle(writer, centerX, headY, headRadius, ICON_COLOR, size);

        // Draw body (arc/semicircle)
        int bodyRadius = size / 4;
        int bodyY = (size * 2) / 3;
        drawSemiCircle(writer, centerX, bodyY, bodyRadius, ICON_COLOR, size);

        return image;
    }

    /**
     * Generates a default avatar image with default size
     * @return JavaFX Image of the default avatar
     */
    public static Image generateDefaultAvatar() {
        return generateDefaultAvatar(DEFAULT_SIZE);
    }

    /**
     * Draws a filled circle
     */
    private static void drawCircle(
        PixelWriter writer,
        int cx,
        int cy,
        int radius,
        Color color,
        int imageSize
    ) {
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                int dx = x - cx;
                int dy = y - cy;
                if (dx * dx + dy * dy <= radius * radius) {
                    writer.setColor(x, y, color);
                }
            }
        }
    }

    /**
     * Draws a filled semicircle (bottom half)
     */
    private static void drawSemiCircle(
        PixelWriter writer,
        int cx,
        int cy,
        int radius,
        Color color,
        int imageSize
    ) {
        for (int y = cy; y < imageSize && y < cy + radius; y++) {
            for (int x = 0; x < imageSize; x++) {
                int dx = x - cx;
                int dy = y - cy;
                if (dx * dx + dy * dy <= radius * radius) {
                    writer.setColor(x, y, color);
                }
            }
        }
    }

    /**
     * Generates a colored avatar based on a string (e.g., patient name)
     * Uses the string hash to determine a consistent color
     * @param text the text to base the color on (e.g., patient name)
     * @param size the size of the image
     * @return JavaFX Image with colored background and initials
     */
    public static Image generateColoredAvatar(String text, int size) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();

        // Generate a color based on the text hash
        int hash = Math.abs(text.hashCode());
        Color bgColor = Color.hsb((hash % 360), 0.5, 0.8);

        // Fill background with generated color
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                writer.setColor(x, y, bgColor);
            }
        }

        return image;
    }

    /**
     * Generates a colored avatar based on patient initials
     * @param firstName patient's first name
     * @param lastName patient's last name
     * @param size the size of the image
     * @return JavaFX Image with colored background
     */
    public static Image generateInitialsAvatar(
        String firstName,
        String lastName,
        int size
    ) {
        String fullName =
            (firstName != null ? firstName : "") +
            (lastName != null ? lastName : "");
        return generateColoredAvatar(fullName, size);
    }
}
