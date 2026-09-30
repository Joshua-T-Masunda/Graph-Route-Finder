package controller;

import algorithm.*;
import navigator.*;

/**
 * Manages execution of A*, Dijkstra and Greedy algorithms.
 * Stores results and provides statistics for UI.
 */
public class AlgorithmManager {

    private static final double SQRT2 = Math.sqrt(2);

    public enum AlgorithmType {
        ASTAR,
        DIJKSTRA,
        GREEDY
    }

    private final GraphGrid grid;

    private final AStarSearch aStar;
    private final DijkstraSearch dijkstra;
    private final GreedyBestFirstSearch greedy;

    private CustomList<GraphNode> aStarPath;
    private CustomList<GraphNode> dijkstraPath;
    private CustomList<GraphNode> greedyPath;

    // Statistics
    private int aStarNodesVisited, dijkstraNodesVisited, greedyNodesVisited;
    private double aStarTimeMs, dijkstraTimeMs, greedyTimeMs;
    private double aStarPathLength, dijkstraPathLength, greedyPathLength;

    /**
     * Creates a manager for one graph grid.
     */
    public AlgorithmManager(GraphGrid grid) {
        this.grid = grid;
        this.aStar = new AStarSearch(grid);
        this.dijkstra = new DijkstraSearch(grid);
        this.greedy = new GreedyBestFirstSearch(grid);
    }

    /**
     * Runs A*, Dijkstra, and Greedy Best-First Search from the same start and
     * goal. The graph is reset between runs so each algorithm receives the same
     * initial state.
     */
    public void computeAll(GraphNode start, GraphNode goal) {
        if (start == null || goal == null) return;

        grid.resetAll();
        this.aStarPath = aStar.findPath(start, goal);
        this.aStarNodesVisited = aStar.getNodesVisited();
        this.aStarTimeMs = aStar.getComputeTimeMs();
        this.aStarPathLength = aStar.getPathLength();

        grid.resetAll();
        this.dijkstraPath = dijkstra.findPath(start, goal);
        this.dijkstraNodesVisited = dijkstra.getNodesVisited();
        this.dijkstraTimeMs = dijkstra.getComputeTimeMs();
        this.dijkstraPathLength = dijkstra.getPathLength();

        grid.resetAll();
        this.greedyPath = greedy.findPath(start, goal);
        this.greedyNodesVisited = greedy.getNodesVisited();
        this.greedyTimeMs = greedy.getComputeTimeMs();
        this.greedyPathLength = greedy.getPathLength();
    }

    /**
     * Returns the path produced by a selected algorithm.
     */
    public CustomList<GraphNode> getPath(AlgorithmType type) {
        switch (type) {
            case ASTAR: return aStarPath;
            case DIJKSTRA: return dijkstraPath;
            case GREEDY: return greedyPath;
            default: return null;
        }
    }

    /**
     * Returns the algorithm with the lowest path cost.
     */
    public AlgorithmType getBestAlgorithm() {
        double bestCost = Double.MAX_VALUE;
        AlgorithmType bestType = null;

        double aCost = calculatePathCost(aStarPath);
        double dCost = calculatePathCost(dijkstraPath);
        double gCost = calculatePathCost(greedyPath);

        if (aCost < bestCost) { bestCost = aCost; bestType = AlgorithmType.ASTAR; }
        if (dCost < bestCost) { bestCost = dCost; bestType = AlgorithmType.DIJKSTRA; }
        if (gCost < bestCost) { bestCost = gCost; bestType = AlgorithmType.GREEDY; }

        return bestType;
    }

    private double calculatePathCost(CustomList<GraphNode> path) {
        if (path == null || path.size() < 2) return Double.MAX_VALUE;

        double totalCost = 0;
        for (int i = 1; i < path.size(); i++) {
            GraphNode from = path.get(i - 1);
            GraphNode to = path.get(i);
            int dx = Math.abs(from.x - to.x);
            int dy = Math.abs(from.y - to.y);
            double stepDistance = (dx == 1 && dy == 1) ? SQRT2 : 1.0;
            totalCost += stepDistance * to.weight;
        }
        return totalCost;
    }

    // Getters
    public CustomList<GraphNode> getAStarPath() { return aStarPath; }
    public CustomList<GraphNode> getDijkstraPath() { return dijkstraPath; }
    public CustomList<GraphNode> getGreedyPath() { return greedyPath; }
    
    public int getAStarNodesVisited() { return aStarNodesVisited; }
    public int getDijkstraNodesVisited() { return dijkstraNodesVisited; }
    public int getGreedyNodesVisited() { return greedyNodesVisited; }
    
    public double getAStarTimeMs() { return aStarTimeMs; }
    public double getDijkstraTimeMs() { return dijkstraTimeMs; }
    public double getGreedyTimeMs() { return greedyTimeMs; }
    
    public double getAStarPathLength() { return aStarPathLength; }
    public double getDijkstraPathLength() { return dijkstraPathLength; }
    public double getGreedyPathLength() { return greedyPathLength; }
}
