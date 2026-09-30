package algorithm;

import navigator.GraphNode;

/**
 * HeuristicType.java
 * Defines and calculates the estimated cost to reach the goal.
 */
public enum HeuristicType {
    MANHATTAN,
    EUCLIDEAN,
    OCTILE;

    /**
     * Calculates the estimated distance between two nodes.
     */
    public double calculate(GraphNode a, GraphNode b) {
        double dx = Math.abs(a.x - b.x);
        double dy = Math.abs(a.y - b.y);

        switch (this) {
            case MANHATTAN:
                // 4-directional: only up, down, left, right
                return dx + dy;

            case EUCLIDEAN:
                // Straight line "as the crow flies"
                return Math.sqrt(dx * dx + dy * dy);

            case OCTILE:
                // 8-directional: allows diagonals. 
                // Cost of diagonal is sqrt(2) approx 1.414
                double D = 1.0;
                double D2 = Math.sqrt(2);
                return D * (dx + dy) + (D2 - 2 * D) * Math.min(dx, dy);

            default:
                return 0;
        }
    }
}