package classifier;

import navigator.CustomList;
import navigator.GraphGrid;
import navigator.GraphNode;

public class GraphSmoother {

    private static final int MIN_AGREERS = 5;
    private static final int POTHOLE_MIN_AGREERS = 7;

    public void smooth(GraphGrid grid) {
        if (grid == null) {
            return;
        }

        KNNClassifier.Label[][] snapshot = new KNNClassifier.Label[grid.width][grid.height];

        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                GraphNode node = grid.getNode(x, y);
                snapshot[x][y] = node != null ? node.terrainLabel : KNNClassifier.Label.UNKNOWN;
            }
        }

        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                GraphNode node = grid.getNode(x, y);
                if (node == null) {
                    continue;
                }

                KNNClassifier.Label majority = getMajorityNeighbourLabel(grid, node, snapshot);
                if (majority != KNNClassifier.Label.UNKNOWN && shouldRelabel(grid, node, majority, snapshot)) {
                    node.setClassification(new KNNClassifier.ClassificationResult(majority, 0.85));
                }
            }
        }
    }

    private KNNClassifier.Label getMajorityNeighbourLabel(GraphGrid grid, GraphNode node,
                                                          KNNClassifier.Label[][] snapshot) {
        CustomList<GraphNode> neighbours = grid.getNeighboursForFeatures(node);
        int[] votes = new int[KNNClassifier.Label.values().length];

        for (int i = 0; i < neighbours.size(); i++) {
            GraphNode neighbour = neighbours.get(i);
            votes[snapshot[neighbour.x][neighbour.y].ordinal()]++;
        }

        int maxVotes = -1;
        KNNClassifier.Label best = KNNClassifier.Label.UNKNOWN;
        for (KNNClassifier.Label label : KNNClassifier.Label.values()) {
            int count = votes[label.ordinal()];
            if (count > maxVotes) {
                maxVotes = count;
                best = label;
            }
        }

        return best;
    }

    private boolean shouldRelabel(GraphGrid grid, GraphNode node, KNNClassifier.Label majority,
                                  KNNClassifier.Label[][] snapshot) {
        if (node.terrainLabel == majority) {
            return false;
        }

        CustomList<GraphNode> neighbours = grid.getNeighboursForFeatures(node);
        int sameCount = 0;

        for (int i = 0; i < neighbours.size(); i++) {
            GraphNode neighbour = neighbours.get(i);
            if (snapshot[neighbour.x][neighbour.y] == majority) {
                sameCount++;
            }
        }

        if (node.terrainLabel == KNNClassifier.Label.POTHOLE) {
            return sameCount >= POTHOLE_MIN_AGREERS;
        }

        return sameCount >= MIN_AGREERS;
    }
}
