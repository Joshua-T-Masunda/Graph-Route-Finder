package histogram;

/**
 * Extracts simple texture signals from a patch.
 */
public class TextureExtractor {

    public double[] extractTextureFeatures(int[][][] patch) {
        return new double[] {
            computeVariance(patch),
            computeEdgeDensity(patch)
        };
    }

    public double computeVariance(int[][][] patch) {
        if (patch == null || patch.length == 0 || patch[0] == null || patch[0].length == 0) {
            return 0.0;
        }

        int height = patch.length;
        int width = patch[0].length;
        int totalPixels = height * width;
        double sum = 0.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sum += toGray(patch[y][x]);
            }
        }

        double mean = sum / totalPixels;
        double variance = 0.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double diff = toGray(patch[y][x]) - mean;
                variance += diff * diff;
            }
        }

        return variance / totalPixels;
    }

    public double computeEdgeDensity(int[][][] patch) {
        if (patch == null || patch.length < 2 || patch[0] == null || patch[0].length < 2) {
            return 0.0;
        }

        int height = patch.length;
        int width = patch[0].length;
        int edgeCount = 0;
        int comparisons = 0;
        final double threshold = 25.0;

        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                double current = toGray(patch[y][x]);
                double right = toGray(patch[y][x + 1]);
                double down = toGray(patch[y + 1][x]);

                if (Math.abs(current - right) > threshold) {
                    edgeCount++;
                }
                if (Math.abs(current - down) > threshold) {
                    edgeCount++;
                }
                comparisons += 2;
            }
        }

        return comparisons == 0 ? 0.0 : (double) edgeCount / comparisons;
    }

    private double toGray(int[] rgb) {
        if (rgb == null || rgb.length < 3) {
            return 0.0;
        }
        return 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
    }
}
