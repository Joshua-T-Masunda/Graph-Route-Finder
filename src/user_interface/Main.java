package user_interface;

import classifier.KNNClassifier;
import controller.AlgorithmManager;
import data.DemoGridGenerator;
import data.ImageLoader;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import navigator.CustomList;
import navigator.GraphGrid;
import navigator.GraphNode;
import navigator.GraphSimilarity;
import navigator.GraphStats;

import java.io.File;
import java.util.function.Consumer;

/**
 * Main class - Entry point of the Hazard-Aware Navigation System.
 * Handles UI setup, user interactions, and interactions between 
 * image loading, classification, and pathfinding.
 */
public class Main extends Application {

    // Palette 
    private static final String APP_BG      = "#0d1117";
    private static final String PANEL_BG    = "#161b22";
    private static final String CARD_BG     = "#1c2230";
    private static final String BORDER      = "#30363d";
    private static final String TEXT        = "#e6edf3";
    private static final String MUTED       = "#8b949e";
    private static final String ACCENT      = "#58a6ff";
    private static final String SUCCESS     = "#3fb950";
    private static final String WARNING     = "#d29922";
    private static final String DANGER      = "#f85149";

    // State
    private GraphGrid          grid;
    private AlgorithmManager   manager;
    private KNNClassifier      currentClassifier;
    private boolean            trainingModeActive;
    private Consumer<GraphNode> trainingCallback;
    private GraphNode          lastTrainingDragNode;
    private int                trainingBrushRadius = 0;
    private int                selectionStep = 1;
    private int                blockSize     = 8;
    private int                kNeighbours   = 5;
    private File               currentMapFile;
    private boolean            currentMapIsDemo;

    // render canvas
    private MapCanvas      mapCanvas;
    private AnimationTimer renderTimer;
    private volatile boolean needsRender = true;

    //Toolbar buttons 
    private Button loadButton;
    private Button demoButton;
    private Button liveButton;
    private Button compareButton;
    private Button trainButton;
    private Button findButton;
    private Button resetButton;

    //Side-panel labels
    private Label statusLabel;
    private Label statusDot;
    private ProgressBar progressBar;
    private Label progressLabel;
 // Statistics labels per algorithm
    private Label graphNodesValue, graphEdgesValue;
    private Label graphWalkableValue, graphBlockedValue;
    private Label graphDensityValue,  graphDegreeValue;
    private Label similarityValue;

    private Label bestPathValue, pathLengthValue;
    private Label pathNodesValue, pathTimeValue;
    private Label aStarLengthValue, aStarNodesValue, aStarTimeValue;
    private Label dijkstraLengthValue, dijkstraNodesValue, dijkstraTimeValue;
    private Label greedyLengthValue, greedyNodesValue, greedyTimeValue;
    private Canvas routeVisualizer;

    private Slider    blockSizeSlider;
    private Spinner<Integer> kSpinner;

    /**
     * Starts the JavaFX application and builds the main window.
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("Graph Route Finder  ·  Pathfinding Visualiser");
        stage.setMinWidth(900);
        stage.setMinHeight(620);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + APP_BG + ";");
        root.setTop(buildToolbar());
        root.setCenter(buildMapArea());
        root.setRight(buildSidePanel());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1180, 760);
        stage.setScene(scene);
        stage.show();

        startRenderLoop();
        flash("Ready  —  load a map or press Demo to begin", ACCENT);
        updateControls();
    }

    @Override
    public void stop() {
        if (renderTimer != null) renderTimer.stop();
    }

    //Toolbar
    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(14, 18, 14, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
            "-fx-background-color: " + PANEL_BG + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 0 0 1 0;"
        );

        // App title with coloured dot
        Label dot   = new Label("●");
        dot.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-size: 10px;");
        Label title = new Label("Graph Route Finder");
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 17px; -fx-font-weight: bold;");
        Label sub   = new Label("Pathfinding Visualiser");
        sub.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        HBox titleBox = new HBox(8, dot, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        VBox branding = new VBox(2, titleBox, sub);
        branding.setAlignment(Pos.CENTER_LEFT);

        // Buttons
        loadButton    = toolbarButton("📂  Load Map",   CARD_BG,  ACCENT);
        demoButton    = toolbarButton("🎲  Demo",       CARD_BG,  ACCENT);
        liveButton    = toolbarButton("Visualization",    CARD_BG,  "#facc15");
        compareButton = toolbarButton("🔍  Compare",    CARD_BG,  "#a371f7");
        trainButton   = toolbarButton("\u25C6  Train",   CARD_BG,  WARNING);
        findButton    = toolbarButton("▶  Find Route",  SUCCESS,  "#ffffff");
        resetButton   = toolbarButton("↺  Reset",       DANGER,   "#ffffff");

        loadButton.setOnAction(e -> loadMap());
        demoButton.setOnAction(e -> loadDemo());
        liveButton.setOnAction(e -> openLiveVisualizer());
        compareButton.setOnAction(e -> compareWithMap());
        trainButton.setOnAction(e -> openTrainingDialog());
        findButton.setOnAction(e -> findRoutes());
        resetButton.setOnAction(e -> resetMapState());

        Separator sepActions = toolbarSeparator();
        Separator sepPrimary = toolbarSeparator();

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(
            branding, spacer,
            loadButton, demoButton, sepActions,
            liveButton, compareButton, trainButton, sepPrimary,
            findButton, resetButton
        );
        return bar;
    }

    private Separator toolbarSeparator() {
        Separator separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
        separator.setStyle("-fx-background-color: " + BORDER + ";");
        return separator;
    }

    //Map area 
    private StackPane buildMapArea() {
        mapCanvas = new MapCanvas(860, 660);
        Canvas canvas = mapCanvas.getCanvas();

        StackPane pane = new StackPane(canvas);
        pane.setPadding(new Insets(14));
        pane.setStyle("-fx-background-color: " + APP_BG + ";");

        canvas.widthProperty().bind(pane.widthProperty().subtract(28));
        canvas.heightProperty().bind(pane.heightProperty().subtract(28));
        canvas.setOnMouseClicked(e -> handleMapClick(e.getX(), e.getY()));
        canvas.setOnMouseDragged(e -> handleTrainingDrag(e.getX(), e.getY()));
        canvas.setOnMouseReleased(e -> lastTrainingDragNode = null);

        pane.widthProperty().addListener((o, ov, nv) -> needsRender = true);
        pane.heightProperty().addListener((o, ov, nv) -> needsRender = true);
        return pane;
    }

    //Side panel
    private VBox buildSidePanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(300);
        panel.setStyle(
            "-fx-background-color: " + PANEL_BG + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 0 0 0 1;"
        );

        // Progress row
        progressLabel = sideLabel("Idle", MUTED, 11, false);
        progressBar   = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + ACCENT + ";");

        panel.getChildren().addAll(
            progressLabel, progressBar,
            buildCard("Graph", buildGraphStats()),
            buildCard("Routes", buildPathStats()),
            buildCard("View", buildViewControls()),
            buildCard("Settings", buildSettingsControls())
        );

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(314);
        scroll.setStyle(
            "-fx-background: " + PANEL_BG + ";" +
            "-fx-background-color: " + PANEL_BG + ";"
        );

        // Wrap in VBox so it fills height
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        wrapper.setPrefWidth(314);
        wrapper.setStyle("-fx-background-color: " + PANEL_BG + ";");
        return wrapper;
    }

    private VBox buildCard(String title, javafx.scene.Node content) {
        Label heading = new Label(title.toUpperCase());
        heading.setStyle(
            "-fx-text-fill: " + MUTED + ";" +
            "-fx-font-size: 10px;" +
            "-fx-font-weight: bold;" +
            "-fx-letter-spacing: 1px;"
        );

        VBox card = new VBox(8, heading, content);
        card.setPadding(new Insets(10, 12, 12, 12));
        card.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1;"
        );
        return card;
    }

    private GridPane buildGraphStats() {
        GridPane g = statGrid();
        graphNodesValue   = addStatRow(g, 0, "Nodes");
        graphEdgesValue   = addStatRow(g, 1, "Edges");
        graphWalkableValue= addStatRow(g, 2, "Walkable");
        graphBlockedValue = addStatRow(g, 3, "Blocked");
        graphDensityValue = addStatRow(g, 4, "Obstacles");
        graphDegreeValue  = addStatRow(g, 5, "Avg degree");
        similarityValue   = addStatRow(g, 6, "Similarity");
        return g;
    }

    private VBox buildPathStats() {
        GridPane g = statGrid();
        bestPathValue   = addStatRow(g, 0, "Best algorithm");
        pathLengthValue = addStatRow(g, 1, "Path length");
        pathNodesValue  = addStatRow(g, 2, "Nodes visited");
        pathTimeValue   = addStatRow(g, 3, "Total time");

        GridPane detail = new GridPane();
        detail.setHgap(8);
        detail.setVgap(5);
        detail.getColumnConstraints().addAll(
            column(34), column(22), column(22), column(22)
        );

        addTableHeader(detail, 0, "Algorithm", "Length", "Nodes", "Time");
        Label[] aStarRow = addAlgorithmRow(detail, 1, "A*", "#ff2d55");
        aStarLengthValue = aStarRow[0];
        aStarNodesValue = aStarRow[1];
        aStarTimeValue = aStarRow[2];

        Label[] dijkstraRow = addAlgorithmRow(detail, 2, "Dijkstra", "#38bdf8");
        dijkstraLengthValue = dijkstraRow[0];
        dijkstraNodesValue = dijkstraRow[1];
        dijkstraTimeValue = dijkstraRow[2];

        Label[] greedyRow = addAlgorithmRow(detail, 3, "Greedy", "#a855f7");
        greedyLengthValue = greedyRow[0];
        greedyNodesValue = greedyRow[1];
        greedyTimeValue = greedyRow[2];

        routeVisualizer = new Canvas(252, 72);
        drawEmptyRouteVisualizer();

        VBox box = new VBox(10, g, new Separator(), detail, routeVisualizer);
        return box;
    }

    private VBox buildViewControls() {
        CheckBox aStarToggle    = check("A*  (rose)",      true);
        CheckBox dijkstraToggle = check("Dijkstra  (cyan)",true);
        CheckBox greedyToggle   = check("Greedy  (purple)",true);
        CheckBox hazardToggle   = check("Terrain overlay", true);
        CheckBox gridToggle     = check("Grid lines",      true);

        aStarToggle.setOnAction(e -> { mapCanvas.setShowAStar(aStarToggle.isSelected());    needsRender = true; });
        dijkstraToggle.setOnAction(e -> { mapCanvas.setShowDijkstra(dijkstraToggle.isSelected()); needsRender = true; });
        greedyToggle.setOnAction(e -> { mapCanvas.setShowGreedy(greedyToggle.isSelected()); needsRender = true; });
        hazardToggle.setOnAction(e -> { mapCanvas.setShowHazards(hazardToggle.isSelected()); needsRender = true; });
        gridToggle.setOnAction(e -> { mapCanvas.setShowGridLines(gridToggle.isSelected()); needsRender = true; });

        VBox box = new VBox(6, aStarToggle, dijkstraToggle, greedyToggle, new Separator(), hazardToggle, gridToggle);
        return box;
    }

    private VBox buildSettingsControls() {
        Label blockLabel = sideLabel("Block size: " + blockSize + " px", TEXT, 12, false);
        blockSizeSlider = new Slider(4, 24, blockSize);
        blockSizeSlider.setMajorTickUnit(4);
        blockSizeSlider.setShowTickMarks(true);
        blockSizeSlider.setStyle("-fx-control-inner-background: " + CARD_BG + ";");
        blockSizeSlider.valueProperty().addListener((o, ov, nv) -> {
            blockSize = nv.intValue();
            blockLabel.setText("Block size: " + blockSize + " px");
        });

        Label kLabel = sideLabel("KNN neighbours", TEXT, 12, false);
        kSpinner = new Spinner<>(1, 15, kNeighbours);
        kSpinner.setMaxWidth(Double.MAX_VALUE);
        kSpinner.valueProperty().addListener((o, ov, nv) -> {
            kNeighbours = nv;
            currentClassifier = null;
            flash("Press Apply to rebuild with K = " + kNeighbours, ACCENT);
        });

        Button apply = pillButton("Apply to Current Map", CARD_BG, ACCENT);
        apply.setMaxWidth(Double.MAX_VALUE);
        apply.setOnAction(e -> rebuildCurrentMapFromSettings());

        VBox box = new VBox(8, blockLabel, blockSizeSlider, kLabel, kSpinner, apply);
        return box;
    }

    //Status bar 
    private HBox buildStatusBar() {
        statusDot   = new Label("●");
        statusDot.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-size: 9px;");
        statusLabel = sideLabel("Ready", ACCENT, 12, false);

        HBox left = new HBox(6, statusDot, statusLabel);
        left.setAlignment(Pos.CENTER_LEFT);

        Label hint = sideLabel("Click map to place  S → E  then Find Route", MUTED, 11, false);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(left, spacer, hint);
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
            "-fx-background-color: " + PANEL_BG + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-width: 1 0 0 0;"
        );
        return bar;
    }

    //Render loop
    private void startRenderLoop() {
        renderTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (needsRender && mapCanvas != null) {
                    mapCanvas.render();
                    needsRender = false;
                }
            }
        };
        renderTimer.start();
    }

    //Map interaction
    private void handleMapClick(double x, double y) {
        GraphNode node = mapCanvas.nodeAtPixel(x, y);
        if (node == null) return;

        if (trainingModeActive && trainingCallback != null) {
            trainingCallback.accept(node);
            lastTrainingDragNode = node;
            needsRender = true;
            return;
        }

        if (!node.walkable) {
            flash("⛔  Choose a walkable cell", DANGER);
            return;
        }

        if (selectionStep == 1) {
            mapCanvas.setStartNode(node);
            mapCanvas.setEndNode(null);
            clearPathDisplay();
            selectionStep = 2;
            flash("📍  Start set  —  click to set End", WARNING);
        } else {
            if (node.equals(mapCanvas.getStartNode())) {
                flash("⚠  End must differ from Start", DANGER);
                return;
            }
            mapCanvas.setEndNode(node);
            selectionStep = 3;
            flash("🏁  End set  —  press Find Route", SUCCESS);
        }

        needsRender = true;
        updateControls();
    }

    private void handleTrainingDrag(double x, double y) {
        if (!trainingModeActive || trainingCallback == null) {
            return;
        }
        GraphNode node = mapCanvas.nodeAtPixel(x, y);
        if (node == null || node == lastTrainingDragNode) {
            return;
        }
        trainingCallback.accept(node);
        lastTrainingDragNode = node;
        needsRender = true;
    }

    //Loading
    private void loadMap() {
        File file = chooseImage("Open Map Image");
        if (file != null) {
            currentMapFile = file;
            currentMapIsDemo = false;
            loadImageAsync(file, true);
        }
    }

    private void loadDemo() {
        loadDemo(false);
    }
    /**
     * Generates a synthetic demo map for testing without real images.
     */
    private void loadDemo(boolean openVisualizer) {
        busy("Generating demo graph…", WARNING);

        Thread worker = new Thread(() -> {
            try {
                GraphGrid demo = DemoGridGenerator.generate(60, 60, blockSize);
                Platform.runLater(() -> {
                    currentMapFile = null;
                    currentMapIsDemo = true;
                    applyGrid(demo, null);
                    if (openVisualizer) {
                        new PathfindingVisualizerWindow().show();
                    }
                });
            } catch (Throwable ex) {
                Platform.runLater(() -> {
                    idle();
                    showError("Demo failed", ex.getMessage());
                });
            }
        }, "demo-loader");
        worker.setDaemon(true);
        worker.start();
    }
    /**
     * Loads a satellite image asynchronously and classifies terrain.
     * Updates the grid and canvas when complete.
     */
    private void loadImageAsync(File file, boolean withImage) {
        busy("Building graph from image…", WARNING);

        Thread worker = new Thread(() -> {
            try {
                ImageLoader loader = new ImageLoader(blockSize, kNeighbours);
                currentClassifier = loader.getClassifier();
                GraphGrid loadedGrid = loader.load(file.getAbsolutePath(), (done, total, msg) ->
                        Platform.runLater(() -> {
                            progressBar.setProgress(total == 0 ? 0 : (double) done / total);
                            progressLabel.setText(msg);
                        }));

                javafx.scene.image.Image img = withImage ? loadPreviewImage(file) : null;

                Platform.runLater(() -> applyGrid(loadedGrid, img));

            } catch (Throwable ex) {
                Platform.runLater(() -> {
                    idle();
                    showError("Load failed", friendlyLoadError(ex));
                });
            }
        }, "map-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void rebuildCurrentMapFromSettings() {
        if (trainingModeActive) {
            flash("Close training mode before rebuilding the map", WARNING);
            return;
        }
        if (currentMapIsDemo) {
            loadDemo(false);
        } else if (currentMapFile != null) {
            loadImageAsync(currentMapFile, true);
        } else {
            flash("Settings will apply to the next loaded map", ACCENT);
        }
    }

    private void openLiveVisualizer() {
        new PathfindingVisualizerWindow().show();
        flash("Live pathfinding visualizer opened", ACCENT);
    }

    private javafx.scene.image.Image loadPreviewImage(File file) {
        return new javafx.scene.image.Image(file.toURI().toString(), 2200, 2200, true, true, false);
    }

    private String friendlyLoadError(Throwable ex) {
        if (ex instanceof OutOfMemoryError) {
            return "The image was too large for the current Java heap. The loader now downsizes images, but this file may still need a larger block size or a smaller source image.";
        }
        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
    }

    private void compareWithMap() {
        if (grid == null) { flash("⚠  Load a map first", DANGER); return; }

        File file = chooseImage("Select Comparison Map");
        if (file == null) return;

        busy("Comparing graphs…", "#a371f7");

        Thread worker = new Thread(() -> {
            try {
                ImageLoader loader = new ImageLoader(blockSize, kNeighbours);
                GraphGrid other   = loader.load(file.getAbsolutePath());
                double    score   = GraphSimilarity.compare(grid, other);

                Platform.runLater(() -> {
                    similarityValue.setText(String.format("%.0f %%", score * 100.0));
                    idle();
                    flash(String.format("✔  Similarity: %.0f %%", score * 100.0), SUCCESS);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> { idle(); showError("Compare failed", ex.getMessage()); });
            }
        }, "compare-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyGrid(GraphGrid loadedGrid, javafx.scene.image.Image image) {
        grid    = loadedGrid;
        manager = new AlgorithmManager(grid);
        mapCanvas.setGrid(grid);
        mapCanvas.setBaseImage(image);
        mapCanvas.setStartNode(null);
        mapCanvas.setEndNode(null);
        mapCanvas.setHighlightedNode(null);
        mapCanvas.setHighlightedNodes(null);
        clearPathDisplay();
        selectionStep = 1;
        progressBar.setProgress(1.0);
        progressLabel.setText(grid.width + " × " + grid.height + " nodes");
        similarityValue.setText("—");
        updateGraphStats();
        flash("✔  Graph ready  (" + grid.width + " × " + grid.height + ")", SUCCESS);
        updateControls();
        needsRender = true;
    }

    /**
     * Runs A*, Dijkstra and Greedy algorithms on the current start/end points.
     * Updates UI with results and statistics.
     */
    private void findRoutes() {
        GraphNode start = mapCanvas.getStartNode();
        GraphNode end   = mapCanvas.getEndNode();
        if (grid == null || start == null || end == null) {
            flash("⚠  Select Start and End first", DANGER);
            return;
        }

        findButton.setDisable(true);
        busy("Running A*  ·  Dijkstra  ·  Greedy…", ACCENT);

        Thread worker = new Thread(() -> {
            manager.computeAll(start, end);
            Platform.runLater(() -> {
                mapCanvas.setAStarPath(manager.getAStarPath());
                mapCanvas.setDijkstraPath(manager.getDijkstraPath());
                mapCanvas.setGreedyPath(manager.getGreedyPath());
                updatePathStats();
                idle();
                flash("✔  Routes computed", SUCCESS);
                updateControls();
                needsRender = true;
            });
        }, "route-worker");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Opens the training dialog for manual and batch label teaching.
     */
    private void openTrainingDialog() {
        if (grid == null) {
            flash("Load a map before training", WARNING);
            return;
        }

        Stage dlg = new Stage();
        dlg.setTitle("Train Map Cells");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: " + APP_BG + ";");

        Label samplesLabel = sideLabel("Saved samples: " + getClassifier().getTrainingSize(), ACCENT, 13, true);
        Label hint         = sideLabel("Choose a label, set brush size, then click or drag on the map.", TEXT, 12, false);

        final KNNClassifier.Label[] active = {KNNClassifier.Label.PATH};
        Label activeLbl = sideLabel("Painting: PATH", SUCCESS, 12, true);
        // Label buttons
        FlowPane labelFlow = new FlowPane(6, 6);
        labelFlow.getChildren().addAll(
            labelBtn("Walkable Path", KNNClassifier.Label.PATH,       active, activeLbl, "#3fb950"),
            labelBtn("Road",      KNNClassifier.Label.ROAD,       active, activeLbl, "#58a6ff"),
            labelBtn("Pothole",   KNNClassifier.Label.POTHOLE,    active, activeLbl, "#f85149"),
            labelBtn("Building/Block", KNNClassifier.Label.STRUCTURE,  active, activeLbl, "#8b949e"),
            labelBtn("Trees",     KNNClassifier.Label.VEGETATION, active, activeLbl, "#56d364"),
            labelBtn("Water",     KNNClassifier.Label.WATER,      active, activeLbl, "#39c5cf")
        );

        Spinner<Integer> brushSpinner = new Spinner<>(1, 5, trainingBrushRadius * 2 + 1, 2);
        brushSpinner.setMaxWidth(90);
        brushSpinner.valueProperty().addListener((o, oldValue, newValue) ->
                trainingBrushRadius = Math.max(0, newValue / 2));
        HBox brushBox = new HBox(8, sideLabel("Brush size", TEXT, 12, false), brushSpinner);
        brushBox.setAlignment(Pos.CENTER_LEFT);

        Button save  = pillButton("Save Training",  CARD_BG, SUCCESS);
        Button clear = pillButton("Reset Samples", CARD_BG, DANGER);
        Button close = pillButton("Done", SUCCESS,  "#ffffff");

        save.setOnAction(e -> {
            getClassifier().saveAllToCSV();
            samplesLabel.setText("Saved samples: " + getClassifier().getTrainingSize());
            save.setStyle(
                "-fx-background-color: " + SUCCESS + ";" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #ffffff;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 5 12;"
            );
            save.setText("Saved");
            Alert saved = new Alert(Alert.AlertType.INFORMATION);
            saved.setTitle("Training Saved");
            saved.setHeaderText(null);
            saved.setContentText("Training samples saved. The painted map is still updated.");
            saved.showAndWait();
            flash("Training saved; painted map stays updated", SUCCESS);
        });
        clear.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Clear saved training samples? Painted labels stay on this map until reload.", ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                getClassifier().clearTrainingData();
                samplesLabel.setText("Saved samples: 0");
                flash("Training samples reset", DANGER);
            }
        });
        close.setOnAction(e -> {
            exitTrainingMode();
            dlg.close();
        });

        HBox actions = new HBox(8, save, clear, close);

        trainingModeActive = true;
        trainingCallback   = node -> {
            int changed = applyTrainingBrush(node, active[0]);
            samplesLabel.setText("Saved samples: " + getClassifier().getTrainingSize());
            hint.setText("Painted " + changed + " " + active[0].name() + " cell(s) at (" + node.x + ", " + node.y + ")");
            updateGraphStats();
            updateControls();
            needsRender = true;
            flash("Painted " + changed + " " + active[0].name() + " cell(s)", SUCCESS);
        };

        dlg.setOnCloseRequest(e -> exitTrainingMode());
        dlg.setOnHidden(e -> exitTrainingMode());

        root.getChildren().addAll(
            sideLabel("Training Workflow", TEXT, 14, true),
            sideLabel("1. Select label  2. Pick brush  3. Paint map  4. Save", MUTED, 11, false),
            new Separator(),
            samplesLabel,
            new Separator(),
            activeLbl, hint, labelFlow, brushBox,
            new Separator(),
            actions
        );

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: " + APP_BG + "; -fx-background-color: " + APP_BG + ";");
        dlg.setScene(new Scene(sp, 520, 320));
        dlg.show();
    }

    private int applyTrainingBrush(GraphNode center, KNNClassifier.Label label) {
        if (center == null || grid == null) {
            return 0;
        }

        CustomList<GraphNode> changed = new CustomList<>();
        int minX = Math.max(0, center.x - trainingBrushRadius);
        int maxX = Math.min(grid.width - 1, center.x + trainingBrushRadius);
        int minY = Math.max(0, center.y - trainingBrushRadius);
        int maxY = Math.min(grid.height - 1, center.y + trainingBrushRadius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                GraphNode node = grid.getNode(x, y);
                if (node == null) continue;
                getClassifier().addTrainingSample(node, label, "manual");
                node.setClassification(new KNNClassifier.ClassificationResult(label, 1.0));
                changed.add(node);
                clearSelectionIfBlocked(node);
            }
        }

        mapCanvas.setHighlightedNodes(changed);
        mapCanvas.setHighlightedNode(center);
        clearPathDisplay();
        return changed.size();
    }

    private void clearSelectionIfBlocked(GraphNode node) {
        if (node.walkable) {
            return;
        }
        if (node == mapCanvas.getStartNode()) {
            mapCanvas.setStartNode(null);
        }
        if (node == mapCanvas.getEndNode()) {
            mapCanvas.setEndNode(null);
        }
        if (mapCanvas.getStartNode() == null) {
            selectionStep = 1;
        } else if (mapCanvas.getEndNode() == null) {
            selectionStep = 2;
        }
    }

    private void exitTrainingMode() {
        trainingModeActive = false;
        trainingCallback = null;
        lastTrainingDragNode = null;
        if (mapCanvas != null) {
            mapCanvas.setHighlightedNode(null);
            mapCanvas.setHighlightedNodes(null);
        }
        needsRender = true;
        updateControls();
    }

    private Button labelBtn(String text, KNNClassifier.Label lbl,
                            KNNClassifier.Label[] active, Label activeLbl, String color) {
        Button b = pillButton(text, CARD_BG, color);
        b.setOnAction(e -> {
            active[0] = lbl;
            activeLbl.setText("Painting: " + lbl.name());
            flash("Label → " + lbl.name(), ACCENT);
        });
        return b;
    }

    //Stats helpers 
    private void updateGraphStats() {
        if (grid == null) return;
        GraphStats s = GraphStats.from(grid);
        graphNodesValue.setText(String.valueOf(s.nodes));
        graphEdgesValue.setText(String.valueOf(s.edges));
        graphWalkableValue.setText(String.valueOf(s.walkable));
        graphBlockedValue.setText(String.valueOf(s.blocked));
        graphDensityValue.setText(String.format("%.1f %%", s.obstacleDensity * 100.0));
        graphDegreeValue.setText(String.format("%.2f", s.averageDegree));
    }

    private void updatePathStats() {
        AlgorithmManager.AlgorithmType best = manager.getBestAlgorithm();
        bestPathValue.setText(best == null ? "—" : best.name());

        if (best != null) {
            double len = getPathLength(best);
            pathLengthValue.setText(len < 0 ? "—" : String.format("%.1f", len));
        } else {
            pathLengthValue.setText("—");
        }

        pathNodesValue.setText(
            "A* " + manager.getAStarNodesVisited() +
            "  D " + manager.getDijkstraNodesVisited() +
            "  G " + manager.getGreedyNodesVisited()
        );
        pathTimeValue.setText(String.format("%.2f ms",
            manager.getAStarTimeMs() + manager.getDijkstraTimeMs() + manager.getGreedyTimeMs()));

        aStarLengthValue.setText(formatLength(manager.getAStarPathLength()));
        aStarNodesValue.setText(String.valueOf(manager.getAStarNodesVisited()));
        aStarTimeValue.setText(formatTime(manager.getAStarTimeMs()));

        dijkstraLengthValue.setText(formatLength(manager.getDijkstraPathLength()));
        dijkstraNodesValue.setText(String.valueOf(manager.getDijkstraNodesVisited()));
        dijkstraTimeValue.setText(formatTime(manager.getDijkstraTimeMs()));

        greedyLengthValue.setText(formatLength(manager.getGreedyPathLength()));
        greedyNodesValue.setText(String.valueOf(manager.getGreedyNodesVisited()));
        greedyTimeValue.setText(formatTime(manager.getGreedyTimeMs()));

        drawRouteVisualizer();
    }

    private double getPathLength(AlgorithmManager.AlgorithmType type) {
        switch (type) {
            case ASTAR:    return manager.getAStarPathLength();
            case DIJKSTRA: return manager.getDijkstraPathLength();
            case GREEDY:   return manager.getGreedyPathLength();
            default:       return -1.0;
        }
    }

    private String formatLength(double value) {
        return value < 0 ? "—" : String.format("%.1f", value);
    }

    private String formatTime(double value) {
        return String.format("%.2f", value);
    }

    private void clearAlgorithmStats() {
        if (aStarLengthValue == null) return;
        aStarLengthValue.setText("—");
        aStarNodesValue.setText("—");
        aStarTimeValue.setText("—");
        dijkstraLengthValue.setText("—");
        dijkstraNodesValue.setText("—");
        dijkstraTimeValue.setText("—");
        greedyLengthValue.setText("—");
        greedyNodesValue.setText("—");
        greedyTimeValue.setText("—");
    }

    private void drawEmptyRouteVisualizer() {
        if (routeVisualizer == null) return;
        GraphicsContext gc = routeVisualizer.getGraphicsContext2D();
        gc.setFill(Color.web(CARD_BG));
        gc.fillRect(0, 0, routeVisualizer.getWidth(), routeVisualizer.getHeight());
        gc.setStroke(Color.web(BORDER));
        gc.strokeRoundRect(0.5, 0.5, routeVisualizer.getWidth() - 1, routeVisualizer.getHeight() - 1, 8, 8);
        gc.setFill(Color.web(MUTED));
        gc.fillText("Run routes to compare algorithms", 36, 42);
    }

    private void drawRouteVisualizer() {
        if (routeVisualizer == null) return;
        GraphicsContext gc = routeVisualizer.getGraphicsContext2D();
        double w = routeVisualizer.getWidth();
        double h = routeVisualizer.getHeight();
        gc.setFill(Color.web(CARD_BG));
        gc.fillRect(0, 0, w, h);
        gc.setStroke(Color.web(BORDER));
        gc.strokeRoundRect(0.5, 0.5, w - 1, h - 1, 8, 8);

        double max = Math.max(manager.getAStarNodesVisited(),
                Math.max(manager.getDijkstraNodesVisited(), manager.getGreedyNodesVisited()));
        if (max <= 0) {
            drawEmptyRouteVisualizer();
            return;
        }

        drawAlgoBar(gc, 14, "A*", manager.getAStarNodesVisited(), max, "#ff2d55");
        drawAlgoBar(gc, 35, "D", manager.getDijkstraNodesVisited(), max, "#38bdf8");
        drawAlgoBar(gc, 56, "G", manager.getGreedyNodesVisited(), max, "#a855f7");
    }

    private void drawAlgoBar(GraphicsContext gc, double y, String label, double value, double max, String color) {
        double x = 30;
        double barW = 166 * (value / max);
        gc.setFill(Color.web(MUTED));
        gc.fillText(label, 10, y + 10);
        gc.setFill(Color.color(1, 1, 1, 0.08));
        gc.fillRoundRect(x, y, 166, 10, 5, 5);
        gc.setFill(Color.web(color));
        gc.fillRoundRect(x, y, Math.max(2, barW), 10, 5, 5);
        gc.setFill(Color.web(TEXT));
        gc.fillText(String.valueOf((int) value), 204, y + 10);
    }

    private void clearPathDisplay() {
        clearAlgorithmStats();
        drawEmptyRouteVisualizer();
        mapCanvas.setAStarPath(null);
        mapCanvas.setDijkstraPath(null);
        mapCanvas.setGreedyPath(null);
        bestPathValue.setText("—");
        pathLengthValue.setText("—");
        pathNodesValue.setText("—");
        pathTimeValue.setText("—");
    }

    // Controls state
    private void updateControls() {
        boolean hasGrid = grid != null;
        findButton.setDisable(!hasGrid || selectionStep < 3);
        compareButton.setDisable(!hasGrid);
        resetButton.setDisable(!hasGrid);
        trainButton.setDisable(false);
    }

    //Progress helpers
    private void busy(String msg, String color) {
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressLabel.setText(msg);
        flash("⏳  " + msg, color);
    }

    private void idle() {
        progressBar.setProgress(0);
        progressLabel.setText("Idle");
    }

    /** Update status label with optional brief fade-in animation. */
    private void flash(String message, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(Color.web(color));
            statusDot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 9px;");
            FadeTransition ft = new FadeTransition(Duration.millis(150), statusLabel);
            ft.setFromValue(0.4);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    //helpers
    private KNNClassifier getClassifier() {
        if (currentClassifier == null) currentClassifier = new KNNClassifier(kNeighbours);
        return currentClassifier;
    }

    private void resetMapState() {
        selectionStep = 1;
        mapCanvas.setStartNode(null);
        mapCanvas.setEndNode(null);
        mapCanvas.setHighlightedNode(null);
        mapCanvas.setHighlightedNodes(null);
        clearPathDisplay();
        needsRender = true;
        flash("↺  Map reset", ACCENT);
        updateControls();
    }

    private File chooseImage(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.jpg","*.jpeg","*.png","*.bmp","*.tif","*.tiff"));
        return fc.showOpenDialog(null);
    }

    private void showError(String header, String body) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(body == null ? "Unknown error." : body);
        alert.showAndWait();
    }

    //Widget factory methods
    private Button toolbarButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6;" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 6;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 7 14;"
        );
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    private Button pillButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: " + fg + ";" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 5 12;"
        );
        return b;
    }

    private GridPane statGrid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(55);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(45);
        g.getColumnConstraints().addAll(c1, c2);
        return g;
    }

    private ColumnConstraints column(double percent) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(percent);
        return c;
    }

    private void addTableHeader(GridPane g, int row, String c0, String c1, String c2, String c3) {
        g.add(sideLabel(c0, MUTED, 10, true), 0, row);
        g.add(sideLabel(c1, MUTED, 10, true), 1, row);
        g.add(sideLabel(c2, MUTED, 10, true), 2, row);
        g.add(sideLabel(c3, MUTED, 10, true), 3, row);
    }

    private Label[] addAlgorithmRow(GridPane g, int row, String name, String color) {
        Label nameLabel = sideLabel(name, color, 11, true);
        Label length = sideLabel("—", TEXT, 10, true);
        Label nodes = sideLabel("—", TEXT, 10, true);
        Label time = sideLabel("—", TEXT, 10, true);
        g.add(nameLabel, 0, row);
        g.add(length, 1, row);
        g.add(nodes, 2, row);
        g.add(time, 3, row);
        return new Label[] { length, nodes, time };
    }

    private Label addStatRow(GridPane g, int row, String key) {
        Label k = sideLabel(key, MUTED, 11, false);
        Label v = sideLabel("—", TEXT, 11, true);
        g.add(k, 0, row);
        g.add(v, 1, row);
        return v;
    }

    private CheckBox check(String text, boolean selected) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        cb.setTextFill(Color.web(TEXT));
        cb.setStyle("-fx-font-size: 12px;");
        return cb;
    }

    private Label sideLabel(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setStyle("-fx-font-size: " + size + "px;" + (bold ? " -fx-font-weight: bold;" : ""));
        l.setWrapText(true);
        return l;
    }

    public static void main(String[] args) { launch(args); }
}








