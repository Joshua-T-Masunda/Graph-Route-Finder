package data;

import classifier.KNNClassifier;
import histogram.FeatureVector;
import histogram.HSVConverter;
import histogram.RichFeatureExtractor;
import navigator.CustomList;
import navigator.GraphGrid;
import navigator.GraphNode;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Loads an image and converts it into a graph of classified image blocks.
 */
public class ImageLoader {

    private static final int MAX_IMAGE_SIDE = 2200;
    private static final int MAX_GRID_CELLS = 120_000;

    private final int blockSize;
    private final KNNClassifier classifier;
    private final RichFeatureExtractor featureExtractor;

    /**
     * Receives progress updates while large images are converted.
     */
    public interface ProgressListener {
        void onProgress(int completed, int total, String message);
    }

    /**
     * Creates an image loader with its own classifier.
     */
    public ImageLoader(int blockSize, int kNeighbours) {
        this.blockSize = blockSize;
        this.classifier = new KNNClassifier(kNeighbours);
        this.featureExtractor = new RichFeatureExtractor();
    }

    /**
     * Creates an image loader that reuses an existing classifier.
     */
    public ImageLoader(int blockSize, KNNClassifier classifier) {
        this.blockSize = blockSize;
        this.classifier = classifier;
        this.featureExtractor = new RichFeatureExtractor();
    }

    /**
     * Returns the classifier used by this loader.
     */
    public KNNClassifier getClassifier() {
        return classifier;
    }

    /**
     * Loads an image without progress updates.
     */
    public GraphGrid load(String filePath) throws IOException {
        return load(filePath, null);
    }

    /**
     * Loads an image, extracts handwritten features, and classifies graph nodes.
     */
    public GraphGrid load(String filePath, ProgressListener progress) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Image not found: " + filePath);
        }

        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Unsupported image format: " + filePath);
        }
        image = resizeForProcessing(image);

        return buildHazardGrid(image, progress);
    }

    private GraphGrid buildHazardGrid(BufferedImage image, ProgressListener progress) throws IOException {
        int effectiveBlockSize = chooseEffectiveBlockSize(image);
        int gridWidth = (int) Math.ceil((double) image.getWidth() / effectiveBlockSize);
        int gridHeight = (int) Math.ceil((double) image.getHeight() / effectiveBlockSize);
        int totalBlocks = gridWidth * gridHeight;
        GraphGrid grid = new GraphGrid(gridWidth, gridHeight, effectiveBlockSize);

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                int startX = x * effectiveBlockSize;
                int startY = y * effectiveBlockSize;
                int[] average = averageBlockRGB(image, startX, startY, effectiveBlockSize);
                GraphNode node = new GraphNode(x, y, average[0], average[1], average[2]);
                node.setFeatureVector(featureExtractor.extractFromNode(node));
                grid.setNode(x, y, node);
            }
        }

        if (progress != null) {
            progress.onProgress(0, totalBlocks, "Calibrating classifier...");
        }
        autoLearnFromImage(grid);

        int processed = 0;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                GraphNode node = grid.getNode(x, y);
                double[] graphFeatures = featureExtractor.extractWithNeighbors(node, grid);
                KNNClassifier.ClassificationResult result = classifier.classifyWithConfidence(graphFeatures);
                node.setClassification(result);

                processed++;
                if (progress != null && (processed % Math.max(1, totalBlocks / 50) == 0 || processed == totalBlocks)) {
                    double percentage = (double) processed / totalBlocks * 100.0;
                    progress.onProgress(processed, totalBlocks, String.format("Classified %.1f%%", percentage));
                }
            }
        }

        if (progress != null) {
            progress.onProgress(totalBlocks, totalBlocks, "Smoothing graph...");
        }
        applySpatialSmoothing(grid);

        return grid;
    }

    /**
     * Creates a small runtime-only training set using handwritten colour rules.
     *
     * <p>This does not save to CSV. It gives Weka labelled examples for the
     * downstream classifier after the graph and features already exist.</p>
     */
    private void autoLearnFromImage(GraphGrid grid) {
        CustomList<FeatureVector> bootstrapData = new CustomList<>();
        int limitPerClass = 25;
        int[] counts = new int[KNNClassifier.Label.values().length];

        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                GraphNode node = grid.getNode(x, y);
                KNNClassifier.Label autoLabel = inferLabelFromColour(node);

                if (autoLabel != null && counts[autoLabel.ordinal()] < limitPerClass) {
                    double[] features = featureExtractor.extractWithNeighbors(node, grid);
                    bootstrapData.add(new FeatureVector(features, autoLabel.name(), "runtime_auto_label"));
                    counts[autoLabel.ordinal()]++;
                }
            }
        }

        classifier.setRuntimeTrainingData(bootstrapData);
    }

    private KNNClassifier.Label inferLabelFromColour(GraphNode node) {
        float[] hsv = HSVConverter.rgbToHsv(node.r, node.g, node.b);
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];

        if (v < 0.25) {
            return KNNClassifier.Label.STRUCTURE;
        }
        if (h > 60 && h < 160 && s > 0.15) {
            return KNNClassifier.Label.VEGETATION;
        }
        if (h >= 15 && h <= 55 && s >= 0.05 && s <= 0.35 && v >= 0.4) {
            return KNNClassifier.Label.ROAD;
        }
        if (v > 0.4 && (s < 0.05 || (h >= 160 && h <= 260))) {
            return KNNClassifier.Label.STRUCTURE;
        }
        if ((h < 15 || h > 340) && s > 0.3 && v > 0.25) {
            return KNNClassifier.Label.STRUCTURE;
        }
        return null;
    }

    /**
     * Smooths isolated classification noise while preserving linear roads and
     * paths, which are important graph corridors for route finding.
     */
    private void applySpatialSmoothing(GraphGrid grid) {
        KNNClassifier.Label[][] newLabels = new KNNClassifier.Label[grid.width][grid.height];

        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                GraphNode node = grid.getNode(x, y);
                if (node == null) {
                    continue;
                }

                KNNClassifier.Label currentLabel = node.terrainLabel;
                CustomList<GraphNode> neighbors = grid.getNeighborsForFeatures(node);
                int[] labelCounts = new int[KNNClassifier.Label.values().length];

                for (int i = 0; i < neighbors.size(); i++) {
                    labelCounts[neighbors.get(i).terrainLabel.ordinal()]++;
                }

                int majorityVotes = 0;
                KNNClassifier.Label majorityLabel = currentLabel;
                for (int i = 0; i < labelCounts.length; i++) {
                    if (labelCounts[i] > majorityVotes) {
                        majorityVotes = labelCounts[i];
                        majorityLabel = KNNClassifier.Label.values()[i];
                    }
                }

                boolean isLinearFeature = currentLabel == KNNClassifier.Label.ROAD
                        || currentLabel == KNNClassifier.Label.PATH;
                boolean isSmallHazard = currentLabel == KNNClassifier.Label.POTHOLE;
                int sameTypeNeighbors = labelCounts[currentLabel.ordinal()];

                if (isLinearFeature && sameTypeNeighbors >= 2) {
                    newLabels[x][y] = currentLabel;
                } else if (isSmallHazard && majorityVotes < 7) {
                    newLabels[x][y] = currentLabel;
                } else if (majorityVotes >= 5 && majorityLabel != currentLabel) {
                    newLabels[x][y] = majorityLabel;
                } else {
                    newLabels[x][y] = currentLabel;
                }
            }
        }

        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                GraphNode node = grid.getNode(x, y);
                if (node != null && newLabels[x][y] != null) {
                    node.setClassification(new KNNClassifier.ClassificationResult(newLabels[x][y], 0.85));
                }
            }
        }
    }

    private int[] averageBlockRGB(BufferedImage image, int startX, int startY, int size) {
        int maxX = Math.min(startX + size, image.getWidth());
        int maxY = Math.min(startY + size, image.getHeight());
        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
        int count = 0;

        for (int y = startY; y < maxY; y++) {
            for (int x = startX; x < maxX; x++) {
                int rgb = image.getRGB(x, y);
                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
                count++;
            }
        }

        if (count == 0) {
            return new int[] {128, 128, 128};
        }

        return new int[] {
            (int) (sumR / count),
            (int) (sumG / count),
            (int) (sumB / count)
        };
    }

    private BufferedImage resizeForProcessing(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= MAX_IMAGE_SIDE) {
            return image;
        }

        double scale = (double) MAX_IMAGE_SIDE / longestSide;
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resized;
    }

    private int chooseEffectiveBlockSize(BufferedImage image) throws IOException {
        int effective = Math.max(4, blockSize);
        while (estimateCellCount(image, effective) > MAX_GRID_CELLS) {
            effective += 2;
            if (effective > 128) {
                throw new IOException("Image is too large to process safely. Try a smaller image.");
            }
        }
        return effective;
    }

    private int estimateCellCount(BufferedImage image, int size) {
        int width = (int) Math.ceil((double) image.getWidth() / size);
        int height = (int) Math.ceil((double) image.getHeight() / size);
        return width * height;
    }
}
