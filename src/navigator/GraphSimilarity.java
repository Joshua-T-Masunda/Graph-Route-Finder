package navigator;

/**
 * Compares two image graphs using graph-level properties.
 */
public class GraphSimilarity {

    /**
     * Returns a similarity score between 0.0 and 1.0.
     */
    public static double compare(GraphGrid first, GraphGrid second) {
        GraphStats a = GraphStats.from(first);
        GraphStats b = GraphStats.from(second);

        if (a.nodes == 0 || b.nodes == 0) {
            return 0.0;
        }

        double labelScore = labelDistributionScore(a, b);
        double obstacleScore = normalizedDifference(a.obstacleDensity, b.obstacleDensity, 1.0);
        double degreeScore = normalizedDifference(a.averageDegree, b.averageDegree, 8.0);
        double weightScore = normalizedDifference(a.averageWeight, b.averageWeight, 15.0);

        return clamp((labelScore * 0.55) + (obstacleScore * 0.20) +
                (degreeScore * 0.15) + (weightScore * 0.10));
    }

    private static double labelDistributionScore(GraphStats a, GraphStats b) {
        double difference = 0.0;

        for (int i = 0; i < a.labelCounts.length; i++) {
            double ap = (double) a.labelCounts[i] / a.nodes;
            double bp = (double) b.labelCounts[i] / b.nodes;
            difference += Math.abs(ap - bp);
        }

        return clamp(1.0 - (difference / 2.0));
    }

    private static double normalizedDifference(double a, double b, double maxRange) {
        if (maxRange <= 0.0) {
            return 0.0;
        }
        return clamp(1.0 - (Math.abs(a - b) / maxRange));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
