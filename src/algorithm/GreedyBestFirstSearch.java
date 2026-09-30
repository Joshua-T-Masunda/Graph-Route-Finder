package algorithm;

import navigator.*;

/**
 * GreedyBestFirstSearch.java - 
 * Features:
 * 1. Heuristic-Only: Prioritizes nodes solely based on distance to the goal (H).
 * 2. Weighted Heuristic: Incorporates terrain weight into the estimate.
 * 3. Corner-Cutting Prevention: Safe diagonal passage.
 * 4. Performance: Extremely fast, though not mathematically optimal.
 */
public class GreedyBestFirstSearch {

    private final GraphGrid grid;
    private static final double SQRT2 = Math.sqrt(2);

    // Search Statistics
    private int nodesVisited;
    private long computeTimeNs;
    private double pathLength;
    private CustomList<GraphNode> visitOrder = new CustomList<>();

    public GreedyBestFirstSearch(GraphGrid grid) {
        this.grid = grid;
    }

    /**
     * Finds a path to the goal by always picking the node that looks "closest."
     */
    public CustomList<GraphNode> findPath(GraphNode start, GraphNode goal) {
        if (start == null || goal == null || !start.walkable || !goal.walkable) {
            return new CustomList<>();
        }

        nodesVisited = 0;
        pathLength = 0.0;
        visitOrder = new CustomList<>();
        grid.resetAll();
        long t0 = System.nanoTime();

        // Greedy uses a Min-Heap based strictly on hCost
        CustomMinHeap<GraphNode> openSet = new CustomMinHeap<>(grid.width * grid.height / 4, true);
        boolean[][] closedSet = new boolean[grid.width][grid.height];

        start.hCost = calculateOctileDistance(start, goal);
        openSet.insert(start.hCost, start);

        while (!openSet.isEmpty()) {
            GraphNode current = openSet.extractMin();

            if (closedSet[current.x][current.y]) continue;
            
            closedSet[current.x][current.y] = true;
            nodesVisited++;
            visitOrder.add(current);

            // Goal check
            if (current.x == goal.x && current.y == goal.y) {
                computeTimeNs = System.nanoTime() - t0;
                CustomList<GraphNode> path = reconstructPath(current);
                this.pathLength = computePhysicalPathLength(path);
                return path;
            }

            CustomList<GraphNode> neighbours = grid.getNeighbors(current);
            for (int i = 0; i < neighbours.size(); i++) {
                GraphNode nb = neighbours.get(i);

                if (closedSet[nb.x][nb.y] || !nb.walkable) continue;

                int dx = nb.x - current.x;
                int dy = nb.y - current.y;

                // --- 1. CORNER CUTTING PREVENTION ---
                if (dx != 0 && dy != 0) {
                    GraphNode side1 = grid.getNode(current.x + dx, current.y);
                    GraphNode side2 = grid.getNode(current.x, current.y + dy);
                    if (side1 == null || side2 == null || !side1.walkable || !side2.walkable) {
                        continue; 
                    }
                }

                // --- 2. WEIGHTED HEURISTIC ---
                // In Greedy, we don't track gCost, but we multiply the hCost 
                // by the node's weight to discourage entering difficult terrain.
                double baseH = calculateOctileDistance(nb, goal);
                double weightedH = baseH * nb.weight;

                if (nb.parent == null && nb != start) { // If not yet visited
                    nb.hCost = weightedH;
                    nb.parent = current;
                    openSet.insert(nb.hCost, nb);
                }
            }
        }

        computeTimeNs = System.nanoTime() - t0;
        pathLength = -1.0;
        return new CustomList<>();
    }

    private double calculateOctileDistance(GraphNode a, GraphNode b) {
        double dx = Math.abs(a.x - b.x);
        double dy = Math.abs(a.y - b.y);
        return Math.max(dx, dy) + (SQRT2 - 1) * Math.min(dx, dy);
    }

    private CustomList<GraphNode> reconstructPath(GraphNode goal) {
        CustomList<GraphNode> path = new CustomList<>();
        GraphNode curr = goal;
        while (curr != null) {
            path.add(0, curr);
            curr = curr.parent;
        }
        return path;
    }

    private double computePhysicalPathLength(CustomList<GraphNode> path) {
        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            GraphNode a = path.get(i - 1);
            GraphNode b = path.get(i);
            int dx = Math.abs(a.x - b.x);
            int dy = Math.abs(a.y - b.y);
            total += (dx == 1 && dy == 1) ? SQRT2 : 1.0;
        }
        return total;
    }

    public int getNodesVisited() { return nodesVisited; }
    public double getComputeTimeMs() { return computeTimeNs / 1_000_000.0; }
    public double getPathLength() { return pathLength; }
    public CustomList<GraphNode> getVisitOrder() { return visitOrder; }
}
