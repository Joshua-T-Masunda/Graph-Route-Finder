package histogram;

import navigator.CustomList;
import navigator.GraphGrid;
import navigator.GraphNode;

/**
 * Produces stable 25-dimensional features:
 * 0-17 HSV histogram, 18 variance, 19 edge density, 20-22 mean HSV, 23-24 neighbour mean H/S.
 */
public class RichFeatureExtractor {

    private final HistogramExtractor histogramExtractor;

    public RichFeatureExtractor() {
        this.histogramExtractor = new HistogramExtractor();
    }

    public double[] extractFromNode(GraphNode node) {
        if (node == null) {
            return new double[25];
        }

        double[] features = new double[25];

        if (node.featureVector != null) {
            System.arraycopy(node.featureVector, 0, features, 0, Math.min(node.featureVector.length, 20));
        } else {
            int[][][] patch = new int[1][1][3];
            patch[0][0][0] = node.r;
            patch[0][0][1] = node.g;
            patch[0][0][2] = node.b;
            double[] combined = histogramExtractor.extractCombinedFeatures(patch);
            System.arraycopy(combined, 0, features, 0, Math.min(combined.length, 20));
        }

        features[20] = node.h;
        features[21] = node.s;
        features[22] = node.v;
        features[23] = node.h;
        features[24] = node.s;

        return features;
    }

    public double[] extractFromPatch(int[][][] patch) {
        double[] features = new double[25];
        if (patch == null || patch.length == 0 || patch[0] == null || patch[0].length == 0) {
            return features;
        }

        double[] combined = histogramExtractor.extractCombinedFeatures(patch);
        System.arraycopy(combined, 0, features, 0, Math.min(combined.length, 20));

        double totalH = 0.0;
        double totalS = 0.0;
        double totalV = 0.0;
        int count = 0;

        for (int y = 0; y < patch.length; y++) {
            for (int x = 0; x < patch[0].length; x++) {
                float[] hsv = HSVConverter.rgbToHsv(patch[y][x][0], patch[y][x][1], patch[y][x][2]);
                totalH += hsv[0] / 360.0;
                totalS += hsv[1];
                totalV += hsv[2];
                count++;
            }
        }

        if (count > 0) {
            features[20] = totalH / count;
            features[21] = totalS / count;
            features[22] = totalV / count;
            features[23] = features[20];
            features[24] = features[21];
        }

        return features;
    }

    public double[] extractWithNeighbors(GraphNode node, GraphGrid grid) {
        double[] features = extractFromNode(node);
        if (node == null || grid == null) {
            return features;
        }

        CustomList<GraphNode> neighbours = grid.getNeighborsForFeatures(node);
        if (neighbours.isEmpty()) {
            return features;
        }

        double sumH = 0.0;
        double sumS = 0.0;
        int count = 0;

        for (int i = 0; i < neighbours.size(); i++) {
            GraphNode neighbour = neighbours.get(i);
            if (neighbour != null) {
                sumH += neighbour.h;
                sumS += neighbour.s;
                count++;
            }
        }

        if (count > 0) {
            features[23] = sumH / count;
            features[24] = sumS / count;
        }

        return features;
    }

    public int getFeatureDimension() {
        return 25;
    }
}
