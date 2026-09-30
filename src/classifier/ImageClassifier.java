package classifier;

import java.awt.image.BufferedImage;

import histogram.DatasetLoader;
import histogram.RichFeatureExtractor;
import navigator.GraphGrid;
import navigator.GraphNode;

public class ImageClassifier {

    private static final int PATCH_SIZE = DatasetLoader.PATCH_SIZE;

    private final RichFeatureExtractor featureExtractor;
    private final KNNClassifier classifier;

    public ImageClassifier(KNNClassifier classifier) {
        this.classifier = classifier;
        this.featureExtractor = new RichFeatureExtractor();
    }

    public GraphGrid classifyImage(BufferedImage image) {
        int cols = image.getWidth() / PATCH_SIZE;
        int rows = image.getHeight() / PATCH_SIZE;
        GraphGrid grid = new GraphGrid(cols, rows, PATCH_SIZE);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int[][][] patch = extractPatch(image, col * PATCH_SIZE, row * PATCH_SIZE);
                int[] avgRGB = averagePatchRGB(patch);

                GraphNode node = new GraphNode(col, row, avgRGB[0], avgRGB[1], avgRGB[2]);
                node.setFeatureVector(featureExtractor.extractFromPatch(patch));

                KNNClassifier.ClassificationResult result =
                        classifier.classifyWithConfidence(featureExtractor.extractFromNode(node));
                node.setClassification(result);

                grid.setNode(col, row, node);
            }
        }

        return grid;
    }

    private int[][][] extractPatch(BufferedImage image, int startX, int startY) {
        int[][][] patch = new int[PATCH_SIZE][PATCH_SIZE][3];
        for (int y = 0; y < PATCH_SIZE; y++) {
            for (int x = 0; x < PATCH_SIZE; x++) {
                int rgb = image.getRGB(startX + x, startY + y);
                patch[y][x][0] = (rgb >> 16) & 0xFF;
                patch[y][x][1] = (rgb >> 8) & 0xFF;
                patch[y][x][2] = rgb & 0xFF;
            }
        }
        return patch;
    }

    private int[] averagePatchRGB(int[][][] patch) {
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;

        for (int y = 0; y < patch.length; y++) {
            for (int x = 0; x < patch[0].length; x++) {
                r += patch[y][x][0];
                g += patch[y][x][1];
                b += patch[y][x][2];
                count++;
            }
        }

        return count == 0 ? new int[] {128, 128, 128}
                : new int[] {(int) (r / count), (int) (g / count), (int) (b / count)};
    }
}
