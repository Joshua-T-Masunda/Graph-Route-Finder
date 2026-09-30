# External Code References

This document records implementation areas that use concepts beyond the core textbook examples. The main graph representation, custom list, custom heap, feature extraction, and graph algorithms remain implemented in Java source code within the project.

## Weka RandomForest Classifier

Relevant files:
- `src/classifier/KNNClassifier.java`
- `dist/weka.jar`

The classifier uses the Weka `RandomForest` class for downstream terrain classification. This implementation follows external Java documentation and research-based implementation examples for using Weka datasets, attributes, instances, and classifiers.

The machine learning library is not used to create graph nodes, graph edges, image features, or graph traversal logic. The project first extracts image features using handwritten Java code and stores those values in graph nodes. Weka then receives those numeric feature vectors and predicts a terrain label such as `ROAD`, `PATH`, `STRUCTURE`, `VEGETATION`, `WATER`, or `POTHOLE`.

In simple terms, the Weka model acts as the final decision maker after the project has already built the graph and calculated the features.

## JavaFX User Interface

Relevant files:
- `src/user_interface/Main.java`
- `src/user_interface/MapCanvas.java`
- `src/user_interface/PathfindingVisualizerWindow.java`

The desktop interface uses JavaFX controls, scenes, stages, canvas drawing, background worker threads, and file chooser dialogs. This implementation follows external JavaFX documentation and standard desktop application examples.

JavaFX is used only for interaction and visualisation. It does not provide the graph algorithms or data structures. The canvas is used to draw the image graph, terrain classes, start/end points, and computed routes.

## Image Loading and Pixel Access

Relevant files:
- `src/data/ImageLoader.java`
- `src/histogram/DatasetLoader.java`

The project uses Java's `javax.imageio.ImageIO` and `BufferedImage` APIs to read image files and access pixel RGB values. This implementation follows standard Java image-processing documentation.

The actual feature extraction is still written in the project source code. The image library only provides pixel data from files.

## Handwritten Image Features

Relevant files:
- `src/histogram/HSVConverter.java`
- `src/histogram/HistogramExtractor.java`
- `src/histogram/TextureExtractor.java`
- `src/histogram/RichFeatureExtractor.java`

The project uses image-processing concepts such as HSV colour conversion, histograms, local variance, edge density, and neighbour context. These are not directly covered in the textbook, but they are required by the mini-project theme because the graph must represent meaningful image regions.

The implementation follows external Java documentation and research-based implementation examples for basic image feature engineering. The code itself is written manually and does not call an ML library for feature extraction.

## Graph Similarity Scoring

Relevant files:
- `src/navigator/GraphStats.java`
- `src/navigator/GraphSimilarity.java`

The similarity feature compares two image graphs using graph-level statistics such as terrain distribution, obstacle density, average degree, and average traversal weight. This is a lightweight project-specific scoring method inspired by graph comparison concepts.

The method is intentionally simple so that it remains understandable for a student-level data structures project. It does not use an external graph library.

Graph Statistics and Similarity Scoring
Relevant files:

src/navigator/GraphStats.java
src/navigator/GraphSimilarity.java

GraphStats computes a summary of graph-level properties including node count, edge count, obstacle density, average degree, average traversal weight, and terrain label distribution. GraphSimilarity uses these summaries to produce a weighted similarity score between two image graphs. The label distribution comparison uses a normalised L1 difference across all terrain classes, and the remaining properties are normalised against fixed maximum ranges before being combined with fixed weights.
This scoring method is a lightweight project-specific design inspired by graph comparison concepts and does not use an external graph library.

## Graph Smoothing

Relevant files:
- `src/classifier/GraphSmoother.java`

After initial terrain classification, a majority-vote smoothing pass reduces isolated misclassified nodes. The pass operates on a frozen label snapshot so that corrections made during the pass do not cascade into each other. This approach follows the concept of non-destructive neighbourhood relabelling used in image segmentation post-processing, which is beyond the core textbook examples.
The smoothing logic is implemented entirely in project source code. It does not call an external library. Pothole nodes use a stricter agreement threshold than standard terrain nodes to avoid incorrectly overriding small but valid features.

## Heuristic Functions

Relevant files:
- `src/algorithm/HeuristicType.java`

The algorithm package supports three admissible heuristics for informed search: Manhattan distance for four-directional grids, Euclidean straight-line distance, and Octile distance for eight-directional movement. The Octile formula uses the standard derivation where diagonal movement costs √2, expressed as D * (dx + dy) + (√2 - 2D) * min(dx, dy). These heuristic formulas follow external algorithm references and standard pathfinding literature beyond the core textbook examples.
The heuristic is used only to estimate remaining cost. All graph construction, node expansion, and path reconstruction logic is implemented in the project source code.