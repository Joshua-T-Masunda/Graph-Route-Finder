package navigator;

import classifier.KNNClassifier;
import histogram.HistogramExtractor;

/**
 * Grid-backed graph representation for image regions.
 *
 * <p>The assignment requires a graph ADT at the core of the application. This
 * class represents each image block as a {@link GraphNode}. Edges are implicit
 * between neighbouring grid cells, similar to the adjacency-list approach
 * discussed in the textbook: a node's adjacent vertices are generated only when
 * needed. Removed edges are stored in a compact bit mask so pathfinding remains
 * fast and memory use stays small.</p>
 */
public class GraphGrid {

    private final GraphNode[][] grid;
    private final int[][] removedEdgeMasks;
    public final int width;
    public final int height;
    public final int blockSize;

    /**
     * Creates an empty graph grid with room for width * height vertices.
     *
     * @param width number of graph columns
     * @param height number of graph rows
     * @param blockSize source image block size represented by one vertex
     */
    public GraphGrid(int width, int height, int blockSize) {
        this.width = width;
        this.height = height;
        this.blockSize = blockSize;
        this.grid = new GraphNode[width][height];
        this.removedEdgeMasks = new int[width][height];
    }

    /**
     * Runs through the entire grid and classifies each node using the KNN model.
     */
    public void classifyAll(KNNClassifier knn, HistogramExtractor extractor, int[][][] fullImageRGB) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                GraphNode node = grid[x][y];
                
                // 1. Extract the patch for this specific grid cell
                int[][][] patch = extractPatchFromFullImage(fullImageRGB, x * blockSize, y * blockSize);
                
                // 2. Get features
                double[] features = extractor.extractHSVHistogram(patch);
                
                // 3. Classify and update the node
                KNNClassifier.ClassificationResult result = knn.classifyWithConfidence(features);
                node.setClassification(result);
            }
        }
    }

    /**
     * Gets valid neighbors for pathfinding. 
     * Supports 8-directional movement (Diagonal).
     */
    public CustomList<GraphNode> getNeighbors(GraphNode node) {
        CustomList<GraphNode> neighbors = new CustomList<>(8);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip the node itself

                int nx = node.x + dx;
                int ny = node.y + dy;

                if (isInBounds(nx, ny)) {
                    GraphNode neighbor = grid[nx][ny];
                    if (neighbor != null && neighbor.walkable && isEdgeEnabled(node, neighbor)) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }
        return neighbors;
    }

    public CustomList<GraphNode> getNeighbours(GraphNode node) {
        return getNeighbors(node);
    }

    /**
     * Specialized version for feature extraction.
     * Returns all adjacent nodes regardless of walkability.
     */
    public CustomList<GraphNode> getNeighborsForFeatures(GraphNode node) {
        CustomList<GraphNode> neighbors = new CustomList<>(8);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; 

                int nx = node.x + dx;
                int ny = node.y + dy;

                if (isInBounds(nx, ny)) {
                    GraphNode neighbor = grid[nx][ny];
                    if (neighbor != null) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }
        return neighbors;
    }

    public CustomList<GraphNode> getNeighboursForFeatures(GraphNode node) {
        return getNeighborsForFeatures(node);
    }
    
    private int[][][] extractPatchFromFullImage(int[][][] fullImage, int startX, int startY) {
        int[][][] patch = new int[blockSize][blockSize][3];
        for (int y = 0; y < blockSize; y++) {
            for (int x = 0; x < blockSize; x++) {
                patch[y][x] = fullImage[startY + y][startX + x];
            }
        }
        return patch;
    }

    /**
     * Returns true when coordinates are inside the grid.
     */
    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Gets a vertex at a grid coordinate, or null if outside the grid.
     */
    public GraphNode getNode(int x, int y) {
        return isInBounds(x, y) ? grid[x][y] : null;
    }

    /**
     * Inserts or replaces a vertex at a coordinate.
     */
    public void setNode(int x, int y, GraphNode node) {
        if (isInBounds(x, y)) grid[x][y] = node;
    }

    /**
     * Convenience insert method matching the graph ADT terminology.
     */
    public void addNode(GraphNode node) {
        if (node != null) {
            setNode(node.x, node.y, node);
        }
    }

    /**
     * Removes a vertex from the graph and returns the previous vertex.
     */
    public GraphNode removeNode(int x, int y) {
        if (!isInBounds(x, y)) {
            return null;
        }
        GraphNode removed = grid[x][y];
        grid[x][y] = null;
        return removed;
    }

    /**
     * Builds the outgoing edges for a vertex. Edges are computed on demand to
     * avoid storing thousands of duplicate objects for image-sized graphs.
     */
    public CustomList<GraphEdge> getEdges(GraphNode node) {
        CustomList<GraphEdge> edges = new CustomList<>(8);
        if (node == null || !node.walkable) {
            return edges;
        }

        CustomList<GraphNode> neighbors = getNeighbors(node);
        for (int i = 0; i < neighbors.size(); i++) {
            GraphNode neighbor = neighbors.get(i);
            edges.add(new GraphEdge(node, neighbor, calculateEdgeWeight(node, neighbor)));
        }

        return edges;
    }

    /**
     * Restores a neighbouring edge if it had previously been removed.
     */
    public GraphEdge addEdge(GraphNode from, GraphNode to) {
        if (from == null || to == null || !from.walkable || !to.walkable) {
            return null;
        }
        if (!areNeighbours(from, to)) {
            return null;
        }
        clearRemovedEdge(from, to);
        clearRemovedEdge(to, from);
        return new GraphEdge(from, to, calculateEdgeWeight(from, to));
    }

    /**
     * Removes a neighbouring edge from traversal.
     */
    public boolean removeEdge(GraphNode from, GraphNode to) {
        if (from == null || to == null || !areNeighbours(from, to)) {
            return false;
        }
        markRemovedEdge(from, to);
        markRemovedEdge(to, from);
        return true;
    }

    /**
     * Updates a standalone edge object's weight. Traversal weights are still
     * derived from the destination node, keeping pathfinding consistent.
     */
    public boolean updateEdgeWeight(GraphEdge edge, double weight) {
        if (edge == null || weight < 0.0) {
            return false;
        }
        edge.setWeight(weight);
        return true;
    }

    private double calculateEdgeWeight(GraphNode from, GraphNode to) {
        int dx = Math.abs(from.x - to.x);
        int dy = Math.abs(from.y - to.y);
        double distance = (dx == 1 && dy == 1) ? Math.sqrt(2) : 1.0;
        return distance * to.weight;
    }

    private boolean areNeighbours(GraphNode from, GraphNode to) {
        return isInBounds(from.x, from.y)
                && isInBounds(to.x, to.y)
                && !(from.x == to.x && from.y == to.y)
                && Math.abs(from.x - to.x) <= 1
                && Math.abs(from.y - to.y) <= 1;
    }

    private boolean isEdgeEnabled(GraphNode from, GraphNode to) {
        if (!areNeighbours(from, to)) {
            return false;
        }
        int bit = directionBit(from, to);
        return (removedEdgeMasks[from.x][from.y] & bit) == 0;
    }

    private void markRemovedEdge(GraphNode from, GraphNode to) {
        removedEdgeMasks[from.x][from.y] |= directionBit(from, to);
    }

    private void clearRemovedEdge(GraphNode from, GraphNode to) {
        removedEdgeMasks[from.x][from.y] &= ~directionBit(from, to);
    }

    private int directionBit(GraphNode from, GraphNode to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        int index = (dy + 1) * 3 + (dx + 1);
        if (index > 4) {
            index--;
        }
        return 1 << index;
    }

    public void resetAll() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] != null) grid[x][y].reset();
            }
        }
    }
    

    
    /**
     * Applies spatial smoothing to reduce classification noise while 
     * preserving roads and paths.
     */
    public void applySpatialSmoothing() {
        KNNClassifier.Label[][] newLabels = new KNNClassifier.Label[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                GraphNode node = grid[x][y];
                if (node == null) continue;

                KNNClassifier.Label currentLabel = node.terrainLabel;
                CustomList<GraphNode> neighbors = getNeighborsForFeatures(node);
                
                int[] labelCounts = new int[KNNClassifier.Label.values().length];
                
                // Count the votes of all surrounding blocks
                for (int i = 0; i < neighbors.size(); i++) {
                    labelCounts[neighbors.get(i).terrainLabel.ordinal()]++;
                }

                // Find what the dominant surrounding terrain is
                int majorityVotes = 0;
                KNNClassifier.Label majorityLabel = currentLabel;
                for (int i = 0; i < labelCounts.length; i++) {
                    if (labelCounts[i] > majorityVotes) {
                        majorityVotes = labelCounts[i];
                        majorityLabel = KNNClassifier.Label.values()[i];
                    }
                }

                // === THE SMART GRAPH LOGIC ===
                // Is this a linear feature like a Road or Path?
                boolean isLinearFeature = (currentLabel == KNNClassifier.Label.ROAD || currentLabel == KNNClassifier.Label.PATH);
                boolean isSmallHazard = currentLabel == KNNClassifier.Label.POTHOLE;
                int sameTypeNeighbors = labelCounts[currentLabel.ordinal()];

                // If it's a road and connects to at least 2 other roads, it forms a line. PROTECT IT.
                if (isLinearFeature && sameTypeNeighbors >= 2) {
                    newLabels[x][y] = currentLabel; 
                }
                // Potholes are small features, so only smooth them away with overwhelming agreement.
                else if (isSmallHazard && majorityVotes < 7) {
                    newLabels[x][y] = currentLabel; 
                } 
                // Otherwise, if the surrounding terrain is overwhelmingly different (e.g., 5+ out of 8 blocks)
                // then it's probably noise, so let the bulldozer smooth it out.
                else if (majorityVotes >= 5 && majorityLabel != currentLabel) {
                    newLabels[x][y] = majorityLabel;
                } 
                // Otherwise, keep the original prediction
                else {
                    newLabels[x][y] = currentLabel;
                }
            }
        }

        // Apply the smoothed results to the final grid
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] != null && newLabels[x][y] != null) {
                    grid[x][y].setClassification(new KNNClassifier.ClassificationResult(newLabels[x][y], 0.85));
                }
            }
        }
    }
    
    
    public int countWalkable() {
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] != null && grid[x][y].walkable) {
                    count++;
                }
            }
        }
        return count;
    }

    public int countBlocked() {
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] != null && !grid[x][y].walkable) {
                    count++;
                }
            }
        }
        return count;
    }

    public double averageWeight() {
        double total = 0.0;
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (grid[x][y] != null && grid[x][y].walkable) {
                    total += grid[x][y].weight;
                    count++;
                }
            }
        }
        return count == 0 ? 0.0 : total / count;
    }

    public double obstacleDensity() {
        return width == 0 || height == 0 ? 0.0 : (double) countBlocked() / (width * height);
    }
    
}
