package user_interface;

import classifier.KNNClassifier;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import navigator.CustomList;
import navigator.GraphGrid;
import navigator.GraphNode;

/**
 * MapCanvas – renders the grid, terrain overlay, algorithm paths and markers.
 *
 * Rendering order (back → front):
 *   1. Dark background / base image
 *   2. Terrain colour overlay
 *   3. Blocked-cell hatching
 *   4. Algorithm paths (Greedy → Dijkstra → A*)
 *   5. Start / End markers
 *   6. Training cell highlight
 *   7. Grid lines (optional)
 *   8. Legend + info overlay
 */
public class MapCanvas {

    // Canvas
    private final Canvas canvas;

    //Data 
    private GraphGrid           grid;
    private Image               baseImage;
    private CustomList<GraphNode> aStarPath;
    private CustomList<GraphNode> dijkstraPath;
    private CustomList<GraphNode> greedyPath;
    private GraphNode           startNode;
    private GraphNode           endNode;
    private GraphNode           highlightedNode;
    private CustomList<GraphNode> highlightedNodes;

    // Visibility flags
    private boolean showAStar     = true;
    private boolean showDijkstra  = true;
    private boolean showGreedy    = true;
    private boolean showHazards   = true;
    private boolean showGridLines = true;

    private double viewX;
    private double viewY;
    private double viewW;
    private double viewH;

    // Terrain palette semi-transparent, layered over image
    private static final Color C_PATH       = Color.color(0.18, 0.78, 0.36, 0.76);
    private static final Color C_ROAD       = Color.color(0.28, 0.86, 0.46, 0.72);
    private static final Color C_POTHOLE    = Color.color(0.70, 0.72, 0.76, 0.70);
    private static final Color C_STRUCTURE  = Color.color(0.48, 0.51, 0.56, 0.72);
    private static final Color C_VEGETATION = Color.color(0.56, 0.59, 0.62, 0.68);
    private static final Color C_WATER      = Color.color(0.42, 0.46, 0.52, 0.72);
    private static final Color C_UNKNOWN    = Color.color(0.38, 0.40, 0.46, 0.55);
    private static final Color C_BLOCKED    = Color.color(0.12, 0.13, 0.15, 0.82);

    //Algorithm path colours 
    private static final Color COL_ASTAR    = Color.web("#ff2d55");   // rose
    private static final Color COL_DIJKSTRA = Color.web("#38bdf8");   // sky-blue
    private static final Color COL_GREEDY   = Color.web("#a855f7");   // purple
    private static final Color COL_HIGHLIGHT = Color.web("#facc15");  // yellow

    //Marker colours 
    private static final Color COL_START    = Color.web("#38bdf8");
    private static final Color COL_END      = Color.web("#f97316");

    // Constructor 
    public MapCanvas(double w, double h) {
        this.canvas = new Canvas(w, h);
    }

    //Getters and setters
    public Canvas getCanvas() { return canvas; }

    public void setGrid(GraphGrid g)                       { this.grid         = g; }
    public void setBaseImage(Image i)                      { this.baseImage    = i; }
    public void setAStarPath(CustomList<GraphNode> p)      { this.aStarPath    = p; }
    public void setDijkstraPath(CustomList<GraphNode> p)   { this.dijkstraPath = p; }
    public void setGreedyPath(CustomList<GraphNode> p)     { this.greedyPath   = p; }
    public void setStartNode(GraphNode n)                  { this.startNode    = n; }
    public void setEndNode(GraphNode n)                    { this.endNode      = n; }
    public void setHighlightedNode(GraphNode n)             { this.highlightedNode = n; }
    public void setHighlightedNodes(CustomList<GraphNode> nodes) { this.highlightedNodes = nodes; }
    public GraphNode getStartNode()                        { return startNode; }
    public GraphNode getEndNode()                          { return endNode; }

    public void setShowAStar(boolean v)     { showAStar     = v; }
    public void setShowDijkstra(boolean v)  { showDijkstra  = v; }
    public void setShowGreedy(boolean v)    { showGreedy    = v; }
    public void setShowHazards(boolean v)   { showHazards   = v; }
    public void setShowGridLines(boolean v) { showGridLines = v; }

 
    /**
     * Main rendering method called every frame.
     * Draws layers from background to foreground.
     */
    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double W = canvas.getWidth();
        double H = canvas.getHeight();

        // Clear
        gc.setFill(Color.web("#0d1117"));
        gc.fillRect(0, 0, W, H);

        if (grid == null) {
            renderPlaceholder(gc, W, H);
            return;
        }

        updateViewBounds(W, H);
        double cw = viewW / grid.width;
        double ch = viewH / grid.height;

        // 1. Base image
        boolean imageReady = baseImage != null && !baseImage.isError()
                && baseImage.getWidth() > 0 && baseImage.getHeight() > 0;
        if (imageReady) {
            gc.drawImage(baseImage, viewX, viewY, viewW, viewH);
        }

        // 2. Terrain overlay. Demo maps have no base image, so always draw their graph.
        if (showHazards || !imageReady) {
            renderTerrain(gc, cw, ch);
        }

        // 3. Algorithm paths (back → front)
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        if (showGreedy   && hasPath(greedyPath))   { gc.setLineWidth(2.5); renderPath(gc, greedyPath,   COL_GREEDY,   cw, ch); }
        if (showDijkstra && hasPath(dijkstraPath)) { gc.setLineWidth(3.0); renderPath(gc, dijkstraPath, COL_DIJKSTRA, cw, ch); }
        if (showAStar    && hasPath(aStarPath))    { gc.setLineWidth(3.5); renderPath(gc, aStarPath,    COL_ASTAR,    cw, ch); }

        // 4. Start / End markers
        if (startNode != null) renderMarker(gc, startNode, cw, ch, COL_START, "S");
        if (endNode   != null) renderMarker(gc, endNode,   cw, ch, COL_END,   "E");

        // 5. Highlight the last manually trained cell so the click is visible.
        if (highlightedNodes != null && !highlightedNodes.isEmpty()) {
            for (int i = 0; i < highlightedNodes.size(); i++) {
                renderCellHighlight(gc, highlightedNodes.get(i), cw, ch);
            }
        } else if (highlightedNode != null) {
            renderCellHighlight(gc, highlightedNode, cw, ch);
        }

        // 6. Grid lines are part of the graph overlay, so hide them with hazards.
        if (showHazards && showGridLines) renderGridLines(gc, cw, ch);

        // 7. Legend is hidden with the overlay to leave the uploaded image clean.
        if (showHazards) renderLegend(gc, W, H);
    }

    //Private render helpers 

    private void renderTerrain(GraphicsContext gc, double cw, double ch) {
        for (int x = 0; x < grid.width; x++) {
            for (int y = 0; y < grid.height; y++) {
                GraphNode n = grid.getNode(x, y);
                if (n == null) continue;
                if (!n.walkable) {
                    gc.setFill(C_BLOCKED);
                } else {
                    gc.setFill(terrainColor(n.terrainLabel));
                }
                gc.fillRect(viewX + x * cw, viewY + y * ch, cw + 0.5, ch + 0.5);
            }
        }
    }

    private void renderPath(GraphicsContext gc,
                            CustomList<GraphNode> path, Color color,
                            double cw, double ch) {
        gc.setStroke(color);
        gc.beginPath();
        GraphNode first = path.get(0);
        gc.moveTo(cx(first, cw), cy(first, ch));
        for (int i = 1; i < path.size(); i++) {
            GraphNode n = path.get(i);
            gc.lineTo(cx(n, cw), cy(n, ch));
        }
        gc.stroke();
    }

    private void renderMarker(GraphicsContext gc,
                              GraphNode n, double cw, double ch,
                              Color color, String label) {
        double x  = cx(n, cw);
        double y  = cy(n, ch);
        double r  = Math.max(10, Math.min(cw, ch) * 0.5);

        // Glow ring
        gc.setStroke(color.deriveColor(0, 1, 1, 0.35));
        gc.setLineWidth(4);
        gc.strokeOval(x - r - 4, y - r - 4, (r + 4) * 2, (r + 4) * 2);

        // Fill
        gc.setFill(color);
        gc.fillOval(x - r, y - r, r * 2, r * 2);

        // Border
        gc.setStroke(Color.web("#0d1117"));
        gc.setLineWidth(1.5);
        gc.strokeOval(x - r, y - r, r * 2, r * 2);

        // Letter
        double fontSize = Math.max(9, r * 0.85);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, fontSize));
        gc.fillText(label, x - fontSize * 0.32, y + fontSize * 0.38);
    }

    private void renderCellHighlight(GraphicsContext gc, GraphNode n, double cw, double ch) {
        double x = viewX + n.x * cw;
        double y = viewY + n.y * ch;
        double inset = Math.max(1.0, Math.min(cw, ch) * 0.08);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(Math.max(2.0, Math.min(cw, ch) * 0.18));
        gc.strokeRect(x + inset, y + inset,
                Math.max(1.0, cw - inset * 2), Math.max(1.0, ch - inset * 2));

        gc.setStroke(COL_HIGHLIGHT);
        gc.setLineWidth(Math.max(1.2, Math.min(cw, ch) * 0.08));
        gc.strokeRect(x + inset * 2, y + inset * 2,
                Math.max(1.0, cw - inset * 4), Math.max(1.0, ch - inset * 4));
    }

    private void renderGridLines(GraphicsContext gc, double cw, double ch) {
        gc.setStroke(Color.color(1, 1, 1, 0.06));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= grid.width;  x++) gc.strokeLine(viewX + x * cw, viewY, viewX + x * cw, viewY + viewH);
        for (int y = 0; y <= grid.height; y++) gc.strokeLine(viewX, viewY + y * ch, viewX + viewW, viewY + y * ch);
    }

    private void renderLegend(GraphicsContext gc, double W, double H) {
        double x   = W - 180;
        double y   = 12;
        double pad = 8;
        double bh  = 14;
        double bw  = 14;
        double lh  = bh + 4;

        // How many rows?
        int rows = 3 + 6; // algo rows + terrain rows
        double boxH = rows * lh + pad * 3 + 14 + 14;

        // Semi-transparent background panel
        gc.setFill(Color.color(0.05, 0.07, 0.10, 0.88));
        gc.setStroke(Color.color(0.2, 0.22, 0.28, 0.9));
        gc.setLineWidth(1);
        gc.fillRoundRect(x - pad, y - pad, 175, boxH, 8, 8);
        gc.strokeRoundRect(x - pad, y - pad, 175, boxH, 8, 8);

        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 10));

        // Section: Algorithms
        gc.setFill(Color.color(0.55, 0.60, 0.68, 1.0));
        gc.fillText("ALGORITHMS", x, y + 10);
        y += 16;

        y = legendRow(gc, x, y, bw, bh, lh, COL_ASTAR,    "A*  (Rose)");
        y = legendRow(gc, x, y, bw, bh, lh, COL_DIJKSTRA, "Dijkstra  (Cyan)");
        y = legendRow(gc, x, y, bw, bh, lh, COL_GREEDY,   "Greedy  (Purple)");

        y += 4;
        gc.setFill(Color.color(0.55, 0.60, 0.68, 1.0));
        gc.fillText("TERRAIN", x, y + 10);
        y += 16;

        KNNClassifier.Label[] terrainLabels = {
            KNNClassifier.Label.PATH,
            KNNClassifier.Label.ROAD,
            KNNClassifier.Label.POTHOLE,
            KNNClassifier.Label.STRUCTURE,
            KNNClassifier.Label.VEGETATION,
            KNNClassifier.Label.WATER
        };

        for (KNNClassifier.Label lbl : terrainLabels) {
            y = legendRow(gc, x, y, bw, bh, lh, terrainColor(lbl), lbl.name());
        }
    }

    /** Draws one legend row and returns the updated y position. */
    private double legendRow(GraphicsContext gc,
                             double x, double y, double bw, double bh, double lh,
                             Color color, String text) {
        gc.setFill(color);
        gc.fillRoundRect(x, y, bw, bh, 3, 3);
        gc.setStroke(Color.color(1, 1, 1, 0.2));
        gc.setLineWidth(0.5);
        gc.strokeRoundRect(x, y, bw, bh, 3, 3);
        gc.setFill(Color.color(0.87, 0.89, 0.93, 1.0));
        gc.fillText(text, x + bw + 6, y + bh - 2);
        return y + lh;
    }

    private void renderPlaceholder(GraphicsContext gc, double W, double H) {
        gc.setFill(Color.web("#161b22"));
        gc.fillRect(0, 0, W, H);

        // Subtle grid pattern
        gc.setStroke(Color.color(1, 1, 1, 0.04));
        gc.setLineWidth(1);
        for (double xi = 0; xi < W; xi += 40) gc.strokeLine(xi, 0, xi, H);
        for (double yi = 0; yi < H; yi += 40) gc.strokeLine(0, yi, W, yi);

        // Centre message
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        gc.setFill(Color.web("#30363d"));
        String msg = "Load a map or press  Demo  to begin";
        gc.fillText(msg, W / 2 - 155, H / 2 - 8);

        gc.setFont(Font.font("Monospace", 12));
        gc.setFill(Color.web("#21262d"));
        gc.fillText("Then click to place  S  →  E  and press  Find Route", W / 2 - 190, H / 2 + 18);
    }

    //Path utilities
    private boolean hasPath(CustomList<GraphNode> path) {
        return path != null && path.size() > 1;
    }

    //Node → pixel 
    private double cx(GraphNode n, double cw) { return viewX + n.x * cw + cw / 2.0; }
    private double cy(GraphNode n, double ch) { return viewY + n.y * ch + ch / 2.0; }

    /**
     * Converts pixel coordinates on canvas to corresponding GraphNode.
     */
    public GraphNode nodeAtPixel(double px, double py) {
        if (grid == null) return null;
        updateViewBounds(canvas.getWidth(), canvas.getHeight());
        if (px < viewX || px > viewX + viewW || py < viewY || py > viewY + viewH) {
            return null;
        }
        double cw = viewW / grid.width;
        double ch = viewH / grid.height;
        int gx = Math.max(0, Math.min(grid.width  - 1, (int)((px - viewX) / cw)));
        int gy = Math.max(0, Math.min(grid.height - 1, (int)((py - viewY) / ch)));
        return grid.getNode(gx, gy);
    }

    private void updateViewBounds(double canvasWidth, double canvasHeight) {
        if (grid == null) {
            viewX = 0;
            viewY = 0;
            viewW = canvasWidth;
            viewH = canvasHeight;
            return;
        }

        double sourceW = baseImage != null ? baseImage.getWidth() : grid.width;
        double sourceH = baseImage != null ? baseImage.getHeight() : grid.height;
        if (sourceW <= 0 || sourceH <= 0) {
            viewX = 0;
            viewY = 0;
            viewW = canvasWidth;
            viewH = canvasHeight;
            return;
        }

        double scale = Math.min(canvasWidth / sourceW, canvasHeight / sourceH);
        viewW = sourceW * scale;
        viewH = sourceH * scale;
        viewX = (canvasWidth - viewW) / 2.0;
        viewY = (canvasHeight - viewH) / 2.0;
    }

    // ── Terrain → colour ──────────────────────────────────────────────────────
    private Color terrainColor(KNNClassifier.Label label) {
        if (label == null) return C_UNKNOWN;
        switch (label) {
            case PATH:       return C_PATH;
            case ROAD:       return C_ROAD;
            case POTHOLE:    return C_POTHOLE;
            case STRUCTURE:  return C_STRUCTURE;
            case VEGETATION: return C_VEGETATION;
            case WATER:      return C_WATER;
            default:         return C_UNKNOWN;
        }
    }
}
