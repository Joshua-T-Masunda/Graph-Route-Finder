package algorithm;

import navigator.*;

public class AStarSearch {

    private final GraphGrid grid;
    private static final double SQRT2 = Math.sqrt(2);
    private static final double MIN_TRAVERSAL_WEIGHT = 0.8;

    private int nodesVisited;
    private long computeTimeNs;
    private double pathLength;
    private CustomList<GraphNode> visitOrder = new CustomList<>();

    public AStarSearch(GraphGrid grid) {
        this.grid = grid;
    }

    public CustomList<GraphNode> findPath(GraphNode start, GraphNode goal) {
        if (start == null || goal == null || !start.walkable || !goal.walkable) {
            return new CustomList<>();
        }

        nodesVisited = 0;
        pathLength = 0.0;
        visitOrder = new CustomList<>();
        grid.resetAll();
        long t0 = System.nanoTime();

        CustomMinHeap<GraphNode> openSet = new CustomMinHeap<>(grid.width * grid.height / 4, true);
        boolean[][] closedSet = new boolean[grid.width][grid.height];

        start.gCost = 0.0;
        start.hCost = calculateOctileDistance(start, goal);
        openSet.insert(start.getFCost(), start);

        while (!openSet.isEmpty()) {
            GraphNode current = openSet.extractMin();

            if (closedSet[current.x][current.y]) continue;
            
            closedSet[current.x][current.y] = true;
            nodesVisited++;
            visitOrder.add(current);

            if (current.x == goal.x && current.y == goal.y) {
                computeTimeNs = System.nanoTime() - t0;
                CustomList<GraphNode> path = reconstructPath(current);
                this.pathLength = computePhysicalPathLength(path);
                return path;
            }

            CustomList<GraphNode> neighbours = grid.getNeighbors(current);
            for (int i = 0; i < neighbours.size(); i++) {
                GraphNode nb = neighbours.get(i);

                if (closedSet[nb.x][nb.y]) continue;
                if (!nb.walkable) continue; 

                int dx = nb.x - current.x;
                int dy = nb.y - current.y;

                if (dx != 0 && dy != 0) {
                    GraphNode side1 = grid.getNode(current.x + dx, current.y);
                    GraphNode side2 = grid.getNode(current.x, current.y + dy);
                    if (side1 == null || side2 == null || !side1.walkable || !side2.walkable) {
                        continue; 
                    }
                }

                double stepDist = (dx != 0 && dy != 0) ? SQRT2 : 1.0;
                double movementCost = stepDist * nb.weight;
                double tentativeG = current.gCost + movementCost;

                if (tentativeG < nb.gCost) {
                    // FIXED: Check if the node is already in the heap
                    boolean isNewNode = (nb.gCost == Double.POSITIVE_INFINITY);
                    
                    nb.gCost = tentativeG;
                    nb.hCost = calculateOctileDistance(nb, goal);
                    nb.parent = current;
                    
                    // FIXED: Insert if new, decrease if existing
                    if (isNewNode) {
                        openSet.insert(nb.getFCost(), nb);
                    } else {
                        openSet.decreaseKey(nb, nb.getFCost());
                    }
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
        double gridDistance = Math.max(dx, dy) + (SQRT2 - 1) * Math.min(dx, dy);
        return gridDistance * MIN_TRAVERSAL_WEIGHT;
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
