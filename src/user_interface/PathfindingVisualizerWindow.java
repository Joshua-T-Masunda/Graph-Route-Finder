package user_interface;

import algorithm.AStarSearch;
import algorithm.DijkstraSearch;
import algorithm.GreedyBestFirstSearch;
import classifier.KNNClassifier;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import navigator.CustomList;
import navigator.GraphGrid;
import navigator.GraphNode;

/**
 * Small interactive pathfinding demo window.
 *
 * <p>The visualizer uses the same graph nodes and route algorithms as the main
 * application, but keeps its own grid so demo experiments do not alter a loaded
 * map.</p>
 */
public class PathfindingVisualizerWindow {

    private static final int ROWS = 25;
    private static final int COLS = 25;
    private static final int CELL_SIZE = 20;

    private final GraphGrid graph = new GraphGrid(COLS, ROWS, CELL_SIZE);
    private final Cell[][] cells = new Cell[ROWS][COLS];
    private final ToggleGroup algorithmGroup = new ToggleGroup();
    private final CustomList<ToggleButton> algorithmButtons = new CustomList<>();

    private Label visitedLabel;
    private Label pathLengthLabel;
    private Label statusLabel;
    private Slider speedSlider;
    private Timeline timeline;
    private boolean draggingWall;

    /**
     * Opens the visualizer in a separate JavaFX stage.
     */
    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Interactive Pathfinding Visualizer");

        buildGraph();

        VBox root = new VBox(12);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #0d1117;");
        root.getChildren().addAll(buildStatsPanel(), buildGrid(), buildControls());

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0d1117; -fx-background-color: #0d1117;");
        Scene scene = new Scene(scroll, 610, 700);
        scene.setOnMouseReleased(e -> draggingWall = false);
        stage.setScene(scene);
        stage.setMinWidth(560);
        stage.setMinHeight(520);
        stage.show();
    }

    private VBox buildStatsPanel() {
        Label title = new Label("Pathfinding Visualizer");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");

        visitedLabel = stat("VISITED", "0");
        pathLengthLabel = stat("PATH LENGTH", "0");
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");

        VBox box = new VBox(8, title, new Separator(), visitedLabel, pathLengthLabel, statusLabel);
        return box;
    }

    private GridPane buildGrid() {
        GridPane pane = new GridPane();
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(12));
        pane.setStyle("-fx-background-color: #161b22; -fx-background-radius: 8;");

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Cell cell = new Cell(row, col);
                cells[row][col] = cell;
                pane.add(cell, col, row);
                attachCellHandlers(cell);
            }
        }

        cells[12][4].setStart();
        cells[12][20].setEnd();
        return pane;
    }

    private VBox buildControls() {
        ToggleButton aStar = toggle("A*", true);
        ToggleButton dijkstra = toggle("Dijkstra", false);
        ToggleButton greedy = toggle("Greedy", false);
        updateAlgorithmButtonStyles();

        HBox algorithms = new HBox(8, label("Algorithm"), aStar, dijkstra, greedy);
        algorithms.setAlignment(Pos.CENTER_LEFT);

        speedSlider = new Slider(1, 100, 55);
        Label speedValue = label("55");
        speedSlider.valueProperty().addListener((obs, oldValue, newValue) ->
                speedValue.setText(String.valueOf(newValue.intValue())));
        HBox speed = new HBox(10, label("Speed"), speedSlider, speedValue);
        speed.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        Button run = action("Run");
        Button clearPath = action("Clear Path");
        Button clearAll = action("Clear All");
        run.setOnAction(e -> runSelectedAlgorithm());
        clearPath.setOnAction(e -> clearSearch());
        clearAll.setOnAction(e -> clearAll());

        HBox actions = new HBox(8, run, clearPath, clearAll);
        return new VBox(12, algorithms, speed, actions);
    }

    private void attachCellHandlers(Cell cell) {
        cell.setOnDragDetected(e -> {
            draggingWall = true;
            cell.startFullDrag();
            cell.toggleWall();
        });
        cell.setOnMouseDragEntered(e -> {
            if (draggingWall) {
                cell.setWall(true);
            }
        });
        cell.setOnMouseClicked(e -> cell.toggleWall());
    }

    private void runSelectedAlgorithm() {
        clearSearch();
        stopTimeline();

        GraphNode start = graph.getNode(4, 12);
        GraphNode end = graph.getNode(20, 12);
        String selected = ((ToggleButton) algorithmGroup.getSelectedToggle()).getText();
        CustomList<GraphNode> visited;
        CustomList<GraphNode> path;
        double pathLength;

        if ("Dijkstra".equals(selected)) {
            DijkstraSearch search = new DijkstraSearch(graph);
            path = search.findPath(start, end);
            visited = search.getVisitOrder();
            pathLength = search.getPathLength();
        } else if ("Greedy".equals(selected)) {
            GreedyBestFirstSearch search = new GreedyBestFirstSearch(graph);
            path = search.findPath(start, end);
            visited = search.getVisitOrder();
            pathLength = search.getPathLength();
        } else {
            AStarSearch search = new AStarSearch(graph);
            path = search.findPath(start, end);
            visited = search.getVisitOrder();
            pathLength = search.getPathLength();
        }

        statusLabel.setText("Searching with " + selected);
        animate(visited, path, pathLength);
    }

    private void animate(CustomList<GraphNode> visited, CustomList<GraphNode> path, double pathLength) {
        double frameMs = Math.max(4, 105 - speedSlider.getValue());
        timeline = new Timeline();

        for (int i = 0; i < visited.size(); i++) {
            GraphNode node = visited.get(i);
            int count = i + 1;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(frameMs * i), e -> {
                mark(node, "VISITED");
                visitedLabel.setText(row("VISITED", String.valueOf(count)));
            }));
        }

        double pathStart = frameMs * visited.size();
        for (int i = 0; i < path.size(); i++) {
            GraphNode node = path.get(i);
            int index = i;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(pathStart + frameMs * i), e -> {
                mark(node, "PATH");
                pathLengthLabel.setText(row("PATH LENGTH", String.valueOf(index)));
            }));
        }

        timeline.setOnFinished(e -> {
            pathLengthLabel.setText(row("PATH LENGTH", pathLength < 0 ? "0" : String.format("%.1f", pathLength)));
            statusLabel.setText(path.isEmpty() ? "No route found" : "Done");
        });
        timeline.play();
    }

    private void mark(GraphNode node, String type) {
        if (node == null) return;
        Cell cell = cells[node.y][node.x];
        if (!cell.isStart && !cell.isEnd && !cell.isWall) {
            cell.setType(type);
        }
    }

    private void clearSearch() {
        stopTimeline();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Cell cell = cells[row][col];
                if (!cell.isWall && !cell.isStart && !cell.isEnd) {
                    cell.setType("EMPTY");
                }
            }
        }
        visitedLabel.setText(row("VISITED", "0"));
        pathLengthLabel.setText(row("PATH LENGTH", "0"));
        statusLabel.setText("Ready");
    }

    private void clearAll() {
        clearSearch();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                cells[row][col].setWall(false);
            }
        }
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void buildGraph() {
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                graph.setNode(x, y, new GraphNode(x, y, 40, 40, 46));
            }
        }
    }

    private ToggleButton toggle(String text, boolean selected) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(algorithmGroup);
        button.setSelected(selected);
        button.selectedProperty().addListener((obs, wasSelected, isSelected) -> updateAlgorithmButtonStyles());
        algorithmButtons.add(button);
        styleAlgorithmButton(button);
        return button;
    }

    private void updateAlgorithmButtonStyles() {
        for (int i = 0; i < algorithmButtons.size(); i++) {
            styleAlgorithmButton(algorithmButtons.get(i));
        }
    }

    private void styleAlgorithmButton(ToggleButton button) {
        boolean selected = button.isSelected();
        button.setStyle(
            "-fx-background-color: " + (selected ? "#facc15" : "#1c2230") + ";" +
            "-fx-text-fill: " + (selected ? "#0d1117" : "#e6edf3") + ";" +
            "-fx-font-weight: bold;" +
            "-fx-border-color: " + (selected ? "#ffffff" : "#30363d") + ";" +
            "-fx-border-width: " + (selected ? "2" : "1") + ";" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;" +
            "-fx-padding: 6 12;"
        );
    }

    private Button action(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 12;");
        return button;
    }

    private Label stat(String name, String value) {
        Label label = new Label(row(name, value));
        label.setStyle("-fx-text-fill: #8b949e; -fx-font-family: monospace; -fx-font-size: 13px;");
        return label;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #e6edf3;");
        return label;
    }

    private String row(String name, String value) {
        return String.format("%-18s %s", name, value);
    }

    private class Cell extends StackPane {
        private final int row;
        private final int col;
        private final Rectangle bg;
        private boolean isWall;
        private boolean isStart;
        private boolean isEnd;

        Cell(int row, int col) {
            this.row = row;
            this.col = col;
            this.bg = new Rectangle(CELL_SIZE, CELL_SIZE);
            bg.setFill(Color.web("#22272e"));
            bg.setStroke(Color.web("#0d1117"));
            getChildren().add(bg);
        }

        void setStart() {
            isStart = true;
            setType("START");
        }

        void setEnd() {
            isEnd = true;
            setType("END");
        }

        void toggleWall() {
            setWall(!isWall);
        }

        void setWall(boolean wall) {
            if (isStart || isEnd) return;
            isWall = wall;
            GraphNode node = graph.getNode(col, row);
            node.setClassification(new KNNClassifier.ClassificationResult(
                    wall ? KNNClassifier.Label.STRUCTURE : KNNClassifier.Label.PATH, 1.0));
            setType(wall ? "WALL" : "EMPTY");
        }

        void setType(String type) {
            switch (type) {
                case "WALL": bg.setFill(Color.web("#d0d7de")); break;
                case "VISITED": bg.setFill(Color.web("#2ea043")); break;
                case "PATH": bg.setFill(Color.web("#58a6ff")); break;
                case "START": bg.setFill(Color.web("#ff2d55")); break;
                case "END": bg.setFill(Color.web("#f97316")); break;
                default: bg.setFill(Color.web("#22272e")); break;
            }
        }
    }
}
