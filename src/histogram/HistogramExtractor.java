package histogram;

/**
 * Extracts normalized HSV histograms and optional texture features from RGB patches.
 */
public class HistogramExtractor {

    private static final int H_BINS = 12;
    private static final int S_BINS = 3;
    private static final int V_BINS = 3;
    private static final int TOTAL_BINS = H_BINS + S_BINS + V_BINS;

    public static final int TOTAL_FEATURES = TOTAL_BINS + 2;

    private final TextureExtractor textureExtractor;

    public HistogramExtractor() {
        this.textureExtractor = new TextureExtractor();
    }

    public double[] extractHSVHistogram(int[][][] patch) {
        if (patch == null || patch.length == 0 || patch[0] == null || patch[0].length == 0) {
            return new double[TOTAL_BINS];
        }

        double[] histogram = new double[TOTAL_BINS];
        int height = patch.length;
        int width = patch[0].length;
        int totalPixels = height * width;

        if (totalPixels == 0) {
            return histogram;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] pixel = patch[y][x];
                if (pixel == null || pixel.length < 3) {
                    continue;
                }

                float[] hsv = HSVConverter.rgbToHsv(pixel[0], pixel[1], pixel[2]);
                float h = hsv[0];
                float s = hsv[1];
                float v = hsv[2];

                int hBin = (int) (h / 360.0 * H_BINS);
                int sBin = (int) (s * S_BINS);
                int vBin = (int) (v * V_BINS);

                if (s < 0.2) {
                    hBin = 0;
                }

                hBin = Math.max(0, Math.min(H_BINS - 1, hBin));
                sBin = Math.max(0, Math.min(S_BINS - 1, sBin));
                vBin = Math.max(0, Math.min(V_BINS - 1, vBin));

                histogram[hBin] += 1;
                histogram[H_BINS + sBin] += 1;
                histogram[H_BINS + S_BINS + vBin] += 1;
            }
        }

        for (int i = 0; i < TOTAL_BINS; i++) {
            histogram[i] /= totalPixels;
        }

        return histogram;
    }

    public double[] extractCombinedFeatures(int[][][] patch) {
        double[] histogram = extractHSVHistogram(patch);
        double[] texture = textureExtractor.extractTextureFeatures(patch);
        double[] combined = new double[histogram.length + texture.length];

        System.arraycopy(histogram, 0, combined, 0, histogram.length);
        System.arraycopy(texture, 0, combined, histogram.length, texture.length);

        return combined;
    }

    public int getHistogramSize() {
        return TOTAL_BINS;
    }
}
