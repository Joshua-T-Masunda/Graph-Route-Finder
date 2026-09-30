package navigator;

import classifier.KNNClassifier;

/**
 * Immutable summary of graph properties shown in the JavaFX side panel.
 */
public class GraphStats {

    public final int nodes;
    public final int edges;
    public final int walkable;
    public final int blocked;
    public final double obstacleDensity;
    public final double averageDegree;
    public final double averageWeight;
    public final int[] labelCounts;

    private GraphStats(int nodes, int edges, int walkable, int blocked,
                       double obstacleDensity, double averageDegree,
                       double averageWeight, int[] labelCounts) {
        this.nodes = nodes;
        this.edges = edges;
        this.walkable = walkable;
        this.blocked = blocked;
        this.obstacleDensity = obstacleDensity;
        this.averageDegree = averageDegree;
        this.averageWeight = averageWeight;
        this.labelCounts = labelCounts;
    }

    /**
     * Computes statistics for the supplied graph.
     */
    public static GraphStats from(GraphGrid grid) {
        if (grid == null) {
            return new GraphStats(0, 0, 0, 0, 0.0, 0.0, 0.0, new int[KNNClassifier.Label.values().length]);
        }

        int nodes = 0;
        int edges = 0;
        int walkable = 0;
        int blocked = 0;
        double totalWeight = 0.0;
        int weightedNodes = 0;
        int[] labelCounts = new int[KNNClassifier.Label.values().length];

        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                GraphNode node = grid.getNode(x, y);
                if (node == null) {
                    continue;
                }

                nodes++;
                labelCounts[node.terrainLabel.ordinal()]++;

                if (node.walkable) {
                    walkable++;
                    totalWeight += node.weight;
                    weightedNodes++;
                    edges += grid.getEdges(node).size();
                } else {
                    blocked++;
                }
            }
        }

        int undirectedEdges = edges / 2;
        double density = nodes == 0 ? 0.0 : (double) blocked / nodes;
        double averageDegree = nodes == 0 ? 0.0 : (double) edges / nodes;
        double averageWeight = weightedNodes == 0 ? 0.0 : totalWeight / weightedNodes;

        return new GraphStats(nodes, undirectedEdges, walkable, blocked, density,
                averageDegree, averageWeight, labelCounts);
    }
}
