package navigator;

/**
 * Lightweight edge object connecting two neighbouring graph nodes.
 */
public class GraphEdge {

    private final GraphNode from;
    private final GraphNode to;
    private double weight;

    /**
     * Creates an edge from one node to another with a traversal weight.
     */
    public GraphEdge(GraphNode from, GraphNode to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    /**
     * Returns the starting endpoint.
     */
    public GraphNode getFrom() {
        return from;
    }

    /**
     * Returns the ending endpoint.
     */
    public GraphNode getTo() {
        return to;
    }

    /**
     * Returns the traversal weight.
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Updates the traversal weight stored in this edge object.
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }
}
