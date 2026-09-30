package navigator;

import classifier.KNNClassifier;

/**
 * Vertex in the image graph.
 *
 * <p>A node represents one image block and stores the data needed for
 * classification, graph statistics, and pathfinding.</p>
 */
public class GraphNode {

    // Position and Color
    public final int x, y;
    public final int r, g, b;
    public double h, s, v;
    public double variance;
    public double edgeDensity;
    public double[] featureVector;
    
    // Classification Results
    public KNNClassifier.Label terrainLabel;
    public KNNClassifier.Label label;
    public double classificationConfidence;
    
    // Navigation Data
    public boolean walkable;
    public double weight;

    // A* Algorithm State
    public double gCost;
    public double hCost;
    public GraphNode parent;

    /**
     * Creates a node from RGB values sampled from an image block.
     */
    public GraphNode(int x, int y, int r, int g, int b) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.g = g;
        this.b = b;
        float[] hsv = histogram.HSVConverter.rgbToHsv(r, g, b);
        this.h = hsv[0] / 360.0;
        this.s = hsv[1];
        this.v = hsv[2];
        
        // Default state before classification
        this.terrainLabel = KNNClassifier.Label.UNKNOWN;
        this.label = this.terrainLabel;
        this.classificationConfidence = 0.0;
        this.walkable = true; 
        this.weight = 1.0;
        
        reset();
    }

    /**
     * Creates a node from HSV values. This constructor supports synthetic demo
     * data while keeping the same public graph representation.
     */
    public GraphNode(int x, int y, double h, double s, double v,
                     boolean walkable, double weight, KNNClassifier.Label label) {
        this(x, y, hsvToRgb(h, s, v)[0], hsvToRgb(h, s, v)[1], hsvToRgb(h, s, v)[2]);
        this.h = h;
        this.s = s;
        this.v = v;
        this.walkable = walkable;
        this.weight = weight;
        this.terrainLabel = label != null ? label : KNNClassifier.Label.UNKNOWN;
        this.label = this.terrainLabel;
    }

    /**
     * Updates the node from a classifier result and refreshes traversal values.
     */
    public void setClassification(KNNClassifier.ClassificationResult result) {
        this.terrainLabel = result.getLabel();
        this.label = this.terrainLabel;
        this.classificationConfidence = result.getConfidence();
        
        // Update walkability and weight based on the label
        this.weight = getWeightByLabel(this.terrainLabel);
        this.walkable = (this.weight < Double.POSITIVE_INFINITY);
    }

    /**
     * Returns the current terrain classification.
     */
    public KNNClassifier.Label getClassificationLabel() {
        return terrainLabel;
    }

    /**
     * Returns the model confidence associated with the classification.
     */
    public double getClassificationConfidence() {
        return classificationConfidence;
    }

    /**
     * Stores the handwritten feature vector for reuse during training.
     */
    public void setFeatureVector(double[] features) {
        this.featureVector = features != null ? features.clone() : null;
        if (this.featureVector != null && this.featureVector.length >= 20) {
            this.variance = this.featureVector[18];
            this.edgeDensity = this.featureVector[19];
        }
    }

    private double getWeightByLabel(KNNClassifier.Label label) {
        switch (label) {
            case ROAD: return 0.8;      // FASTEST
            case PATH: return 1.0;       // NORMAL  
            case POTHOLE: return 5.0;
            case VEGETATION: return 15.0; // VERY SLOW
            case STRUCTURE: return Double.POSITIVE_INFINITY; // BLOCKED
            case WATER: return Double.POSITIVE_INFINITY;     // BLOCKED
            default: return 10.0;
        }
    }

    /**
     * Returns f = g + h for priority-queue based search.
     */
    public double getFCost() {
        return gCost + hCost;
    }

    /**
     * Resets search state for a new pathfinding request.
     */
    public void reset() {
        this.gCost = Double.POSITIVE_INFINITY;
        this.hCost = 0;
        this.parent = null;
    }

    /**
     * Resets both search state and terrain state.
     */
    public void fullReset() {
        reset();
        this.terrainLabel = KNNClassifier.Label.UNKNOWN;
        this.label = this.terrainLabel;
        this.weight = 1.0;
        this.walkable = true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GraphNode)) {
            return false;
        }
        GraphNode node = (GraphNode) other;
        return x == node.x && y == node.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    private static int[] hsvToRgb(double h, double s, double v) {
        double hue = h > 1.0 ? h : h * 360.0;
        int hi = (int) Math.floor(hue / 60.0) % 6;
        double f = hue / 60.0 - Math.floor(hue / 60.0);
        double p = v * (1.0 - s);
        double q = v * (1.0 - f * s);
        double t = v * (1.0 - (1.0 - f) * s);

        double r1;
        double g1;
        double b1;
        switch (hi) {
            case 0: r1 = v; g1 = t; b1 = p; break;
            case 1: r1 = q; g1 = v; b1 = p; break;
            case 2: r1 = p; g1 = v; b1 = t; break;
            case 3: r1 = p; g1 = q; b1 = v; break;
            case 4: r1 = t; g1 = p; b1 = v; break;
            default: r1 = v; g1 = p; b1 = q; break;
        }

        return new int[] {
            (int) Math.round(Math.max(0, Math.min(1, r1)) * 255),
            (int) Math.round(Math.max(0, Math.min(1, g1)) * 255),
            (int) Math.round(Math.max(0, Math.min(1, b1)) * 255)
        };
    }
}
