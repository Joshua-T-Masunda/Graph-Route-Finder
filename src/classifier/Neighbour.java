package classifier;

public class Neighbour implements Comparable<Neighbour> {

    private double distance;
    private KNNClassifier.Label label;

    public Neighbour(double distance, KNNClassifier.Label label) {
        this.distance = distance;
        this.label = label;
    }

    @Override
    public int compareTo(Neighbour other) {
        return Double.compare(this.distance, other.distance);
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public KNNClassifier.Label getLabel() {
        return label;
    }

    public void setLabel(KNNClassifier.Label label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "Neighbour[label=" + label + ", distance=" + distance + "]";
    }
}
