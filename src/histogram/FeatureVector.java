package histogram;

/**
 * FeatureVector.java
 * 
 * Enhanced to support rich features (histogram + texture + context).
 * Compatible with RichFeatureExtractor and KNNClassifier.
 */
public class FeatureVector {

    private double[] features;     // Full concatenated feature vector
    private String label;
    private String source;

    // Sub-components for easy access/debugging
    private double[] histogram;    // First 18 elements = HSV histogram
    private double[] meanRGB;      // Kept for backward-compatible debug output

    /**
     * Main constructor for rich features
     */
    public FeatureVector(double[] features, String label, String source) {
        this.features = features != null ? features.clone() : new double[0];
        this.label = label != null ? label.toUpperCase() : "UNKNOWN";
        this.source = source != null ? source : "unknown";
        extractSubFeatures();
    }

    /**
     * Simple constructor for backward compatibility (histogram only)
     */
    public FeatureVector(double[] histogramOnly, String label) {
        this(histogramOnly, label, "unknown");
    }

    /**
     * Extract histogram and meanRGB from the full feature array
     */
    private void extractSubFeatures() {
        if (features == null || features.length < 18) {
            this.histogram = features != null ? features.clone() : new double[18];
            this.meanRGB = new double[]{0.5, 0.5, 0.5};
            return;
        }

        // First 18 = HSV Histogram
        this.histogram = new double[18];
        System.arraycopy(features, 0, this.histogram, 0, 18);

        // 20-22 = Mean HSV in the current 25D layout, exposed through the old getter name.
        if (features.length >= 23) {
            this.meanRGB = new double[]{features[20], features[21], features[22]};
        } else {
            this.meanRGB = new double[]{0.5, 0.5, 0.5};
        }
    }

    // Getters

    public double[] getFeatures() {
        return features != null ? features.clone() : new double[0];
    }

    public String getLabel() {
        return label;
    }

    public String getSource() {
        return source;
    }

    public double[] getHistogram() {
        return histogram != null ? histogram.clone() : new double[18];
    }

    public double[] getMeanRGB() {
        return meanRGB != null ? meanRGB.clone() : new double[]{0.5, 0.5, 0.5};
    }

    public int getDimension() {
        return features != null ? features.length : 0;
    }

    // Setters 

    public void setFeatures(double[] features) {
        this.features = features != null ? features.clone() : new double[0];
        extractSubFeatures();
    }

    public void setLabel(String label) {
        this.label = label != null ? label.toUpperCase() : "UNKNOWN";
    }

    public void setSource(String source) {
        this.source = source != null ? source : "unknown";
    }

    // toString()
    @Override
    public String toString() {
        return "FeatureVector[" +
                "dim=" + getDimension() +
                ", label=" + label +
                ", source=" + source +
                ", meanRGB=(" +
                (meanRGB != null && meanRGB.length >= 3 
                    ? String.format("%.2f,%.2f,%.2f", meanRGB[0], meanRGB[1], meanRGB[2]) 
                    : "N/A") +
                ")]";
    }
}
