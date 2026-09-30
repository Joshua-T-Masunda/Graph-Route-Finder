# Graph Route Finder
- JavaFX desktop application for converting map/satellite images into graph grids, classifying terrain cells, comparing maps, and visualising pathfinding algorithms.

## Youtube video link
- https://youtu.be/91B3uDSFwfQ

## Project Structure
- |---ALL_STAR_MINI_PROJECT_2026/
  |---bin/
  |---dist/
    |---AllStar.jar
    |---run.bat
  |---data/
  |---doc/
    |---javadocs
    |---sources.txt
  |---lib/
    |---weka.jar
  |---src/
    |---algorithm/
      |---AStarSearch.java
      |---DijkstraSearch.java
      |---GreedyBestFirstSearch.java
      |---HeuristicType.java
    |---classifier/
      |---DatasetPreprocessor.java
      |---GraphSmoother.java
      |---ImageClassifier.java
      |---KNNClassifier.java
      |---Neighbour.java
    |---controller/
      |---AlgorithmManager.java
    |---data/
      |---DemoGridGenerator.java
      |---ImageLoader.java
    |---histogram/
      |---DatasetLoader.java
      |---DistanceMetric.java
      |---FeatureVector.java
      |---HistogramExtractor.java
      |---HSVConvertor.java
      |---RichFeatureExtractor.java
      |---TextureExtractor.java
      |---Visualizer.java
    |---navigator/
      |---CustomList.java
      |---CustomMinHeap.java
      |---GraphEdge.java
      |---GraphGrid.java
      |---GraphNode.java
      |---GraphSimilar.java
      |---GraphState.java
    |---user_interface/
      |---Main.java
      |---MapCanvas.jar
      |---PathfindingVisualizerWindow.java



## Requirements
- Java JDK 21 or compatible
- JavaFX SDK 21
- `dist/weka.jar` included in this project

Update the JavaFX path in the commands below if your SDK is installed elsewhere.

## Run From Terminal



IMPORTANT PREREQUISITE FOR EVALUATORS
This project utilizes JavaFX. Before executing the commands below, please replace `<PATH_TO_JAVAFX_LIB>` with the absolute path to the `lib` folder of your local JavaFX SDK installation (e.g., `"C:\path\to\javafx-sdk-21\lib"`).

### Option 1: Run the Pre-packaged JAR (Quickest)
To run the compiled application directly, use the following command. Note that it allocates 2GB of RAM (`-Xmx2G`) to handle the Weka machine learning classification:

```powershell
java -Xmx2G --module-path "<PATH_TO_JAVAFX_LIB>" --add-modules javafx.controls,javafx.graphics,javafx.fxml -jar .\dist\allstar.jar

```

### Option 2:Compile and Run from source


Compile:

```powershell
$sources = Get-ChildItem .\src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.graphics,javafx.fxml -cp ".\dist\weka.jar" -d .\out $sources
```

Run:

```powershell
java --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.graphics,javafx.fxml -cp ".\out;.\dist\weka.jar" user_interface.Main
```

If using Eclipse, add JavaFX to the module path, add `dist/weka.jar` to the build path, and run `user_interface.Main`.

## Main Features

- **Load Map**: loads an image and converts it into a graph of cells.
- **Demo**: creates a synthetic demo map.
- **Visualization**: opens an interactive pathfinding visualizer with draggable obstacles.
- **Compare**: compares the current graph with another image graph.
- **Train**: paint map cells with labels such as path, road, building, trees, water, or pothole.
- **Find Route**: runs A*, Dijkstra, and Greedy Best-First Search.
- **Dashboard**: shows graph statistics, best algorithm, path length, visited nodes, and runtime.

## Basic Workflow

1. Click `Load Map` or `Demo`.
2. Click a walkable cell to set the start point.
3. Click another walkable cell to set the destination.
4. Click `Find Route`.
5. Use the dashboard to compare A*, Dijkstra, and Greedy results.

## Visualization Workflow

Click `Visualization` to open the live pathfinding visualizer.

- Select `A*`, `Dijkstra`, or `Greedy`.
- Click or drag cells to add/remove walls.
- Use the speed slider to control animation speed.
- Click `Run` to animate visited cells and the final path.
- Use `Clear Path` to remove only the search result.
- Use `Clear All` to remove walls and reset the grid.

This window is for interactive algorithm demonstration and does not change the loaded map in the main application.

## Training Workflow

1. Load a map first.
2. Click `Train`.
3. Select a terrain label.
4. Choose brush size.
5. Click or drag on the map to paint cells.
6. Click `Save Training`.
7. Click `Done` to return to normal start/end selection.

Painted cells update the current map immediately. Saved training samples are stored in `persistent_training.csv`.

## Important Notes

- Weka is disabled by default at runtime to avoid Java 21 reflective-access errors.
- The app uses its handwritten k-NN fallback classifier unless Weka is explicitly enabled.
- To try Weka manually, run with:

```powershell
java --add-opens java.base/java.lang=ALL-UNNAMED -DminiProject.useWeka=true --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.graphics,javafx.fxml -cp ".\out;.\dist\weka.jar" user_interface.Main
```

- Very large images are downscaled before processing to prevent memory crashes.
- Changing block size or KNN neighbours only takes effect after clicking `Apply to Current Map`.
