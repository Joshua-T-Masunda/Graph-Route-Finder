package histogram;

import classifier.KNNClassifier;
import navigator.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Visualization.java
 * Creates a visual overlay based on classification labels and confidence levels.
 */
public class Visualization {

    /**
     * Saves a classification overlay onto the original image.
     */
    public static void saveOverlay(BufferedImage original,
                                   GraphGrid grid,
                                   String outputPath) {

        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();

        // 1. Draw the original background image
        g.drawImage(original, 0, 0, null);

        int blockSize = grid.blockSize;

        // 2. Iterate through the grid nodes
        for (int gx = 0; gx < grid.width; gx++) {
            for (int gy = 0; gy < grid.height; gy++) {

                GraphNode node = grid.getNode(gx, gy);
                if (node == null) continue;

                // We assume GraphNode now stores the Label from our KNN
                KNNClassifier.Label label = node.getClassificationLabel(); 
                double confidence = node.getClassificationConfidence();

                Color baseColor = getColorForLabel(label);

                // 3. Set transparency based on confidence (0.0 to 1.0)
                // We map confidence 0-100% to alpha 30-160 for a nice look
                int alpha = 30 + (int) (confidence * 130);
                
                int px = gx * blockSize;
                int py = gy * blockSize;

                g.setColor(new Color(
                        baseColor.getRed(),
                        baseColor.getGreen(),
                        baseColor.getBlue(),
                        alpha
                ));

                g.fillRect(px, py, blockSize, blockSize);
                
                // Optional: Draw a thin border around nodes to see the grid
                g.setColor(new Color(255, 255, 255, 40));
                g.drawRect(px, py, blockSize, blockSize);
            }
        }

        g.dispose();

        try {
            ImageIO.write(output, "png", new File(outputPath));
            System.out.println("Visualization saved: " + outputPath);
        } catch (Exception e) {
            System.err.println("Failed to save visualization overlay.");
        }
    }

    /**
     * Definitive color mapping for terrain types.
     */
    private static Color getColorForLabel(KNNClassifier.Label label) {
        if (label == null) return Color.GRAY;

        switch (label) {
            case PATH: return new Color(87, 179, 107);
            case ROAD: return new Color(41, 46, 56);
            case POTHOLE: return new Color(235, 122, 46);
            case STRUCTURE: return new Color(158, 168, 184);
            case VEGETATION: return new Color(41, 128, 64);
            case WATER: return new Color(56, 115, 191);
            case UNKNOWN: return new Color(140, 107, 184);
            default: return Color.BLACK;
        }
    }
}
