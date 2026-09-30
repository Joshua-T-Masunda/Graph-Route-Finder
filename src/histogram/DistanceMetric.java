package histogram;

/**
 * DistanceMetric.java
 * Upgraded to use Manhattan Distance (L1 Norm) which is mathematically superior 
 * for high-dimensional (22D) Rich Feature Vectors, reducing classification errors.
 */
public class DistanceMetric {

    // Advanced: Manhattan is better for histograms and >20 dimensions
    public static double calculateDistance(double[] a, double[] b) {
        return manhattan(a, b); 
    }

    public static double euclidean(double[] a, double[] b) {
        return Math.sqrt(euclideanSquared(a, b));
    }

    public static double euclideanSquared(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return Double.MAX_VALUE;
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    public static double manhattan(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return Double.MAX_VALUE;
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum;
    }
}