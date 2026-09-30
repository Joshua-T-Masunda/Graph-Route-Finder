package data;

import navigator.CustomList;
import navigator.GraphGrid;
import navigator.GraphNode;
import classifier.KNNClassifier;
import histogram.FeatureVector;
import histogram.RichFeatureExtractor;

import java.util.Random;

/**
 * Builds a synthetic settlement graph for demonstrations and manual testing.
 */
public class DemoGridGenerator {

    private static final Random RNG = new Random(2026);  // CS3A seed
    private static final RichFeatureExtractor featureExtractor = new RichFeatureExtractor();

    /**
     * Generates a graph with roads, paths, structures, vegetation, and water.
     */
    public static GraphGrid generate(int width, int height, int blockSize) {
        GraphGrid grid = new GraphGrid(width, height, blockSize);
        boolean[][] walkability = buildSettlementLayout(width, height);
        KNNClassifier.Label[][] groundTruthLabels = new KNNClassifier.Label[width][height];

        // PASS 1: Generate the Grid Nodes and Ground Truth (Raw RGB) ---
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Object[] terrain = generateRealisticTerrain(walkability[x][y], x, y, width, height);
                int[] rgb = (int[]) terrain[0];
                groundTruthLabels[x][y] = (KNNClassifier.Label) terrain[1];
                
                GraphNode node = new GraphNode(x, y, rgb[0], rgb[1], rgb[2]);
                grid.setNode(x, y, node);
            }
        }

        //PASS 2: Generate True 25D Contextual Training Data ---
        // We sample nodes directly from the connected graph so the AI learns neighbor relationships
        CustomList<FeatureVector> trainingData = new CustomList<>();
        
        for (int x = 0; x < width; x += 3) { // Sample every 3rd node for training
            for (int y = 0; y < height; y += 3) {
                GraphNode node = grid.getNode(x, y);
                // EXTRACT 25D VECTOR (Including Neighbors)
                double[] features25D = featureExtractor.extractWithNeighbors(node, grid);
                trainingData.add(new FeatureVector(features25D, groundTruthLabels[x][y].name(), "demo_25D_synth"));
            }
        }

        // Initialize the Classifier and inject the pristine 25D data
        KNNClassifier demoClassifier = new KNNClassifier(5);
        demoClassifier.setRuntimeTrainingData(trainingData);

        // PASS 3: Graph-Based Classification ---
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                GraphNode node = grid.getNode(x, y);
                double[] features25D = featureExtractor.extractWithNeighbors(node, grid);
                
                KNNClassifier.ClassificationResult result = demoClassifier.classifyWithConfidence(features25D);
                node.setClassification(result);
            }
        }

        // PASS 4: Apply Moodley's Spatial Smoother ---
        grid.applySpatialSmoothing();

        return grid;
    }

    //REALISTIC SETTLEMENT LAYOUT

    private static boolean[][] buildSettlementLayout(int W, int H) {
        boolean[][] map = new boolean[W][H];
        int[] hRoads = {H / 6, H / 2, H * 5 / 6};
        int[] vRoads = {W / 5, W / 2, W * 4 / 5};

        for (int road : hRoads) {
            for (int x = 0; x < W; x++) {
                for (int dy = -1; dy <= 1; dy++) if (road + dy >= 0 && road + dy < H) map[x][road + dy] = true;
            }
        }
        for (int road : vRoads) {
            for (int y = 0; y < H; y++) {
                for (int dx = -1; dx <= 1; dx++) if (road + dx >= 0 && road + dx < W) map[road + dx][y] = true;
            }
        }
        for (int h : hRoads) for (int v : vRoads) addWindingAlley(map, v, h, W, H);
        addDiagonalPaths(map, W, H);
        return map;
    }

    private static void addWindingAlley(boolean[][] map, int sx, int sy, int W, int H) {
        int x = sx, y = sy;
        int[][] dirs = {{0,1},{1,0},{0,-1},{-1,0}};
        for (int step = 0; step < 40; step++) {
            int dir = RNG.nextInt(4);
            x = Math.max(0, Math.min(W - 1, x + dirs[dir][0]));
            y = Math.max(0, Math.min(H - 1, y + dirs[dir][1]));
            map[x][y] = true;
            if (RNG.nextBoolean() && x + 1 < W) map[x + 1][y] = true;
        }
    }

    private static void addDiagonalPaths(boolean[][] map, int W, int H) {
        int steps = Math.min(W, H) / 3;
        for (int i = 0; i < steps; i++) {
            int x1 = W / 4 + i * W / 12, y1 = H / 4 + i * H / 12;
            if (x1 + 1 < W && y1 + 1 < H) map[x1][y1] = map[x1 + 1][y1 + 1] = true;
        }
    }

    //TERRAIN & LABEL GENERATION 

    private static Object[] generateRealisticTerrain(boolean walkable, int x, int y, int W, int H) {
        if (walkable) {
            int[] rgb = {140 + RNG.nextInt(30), 105 + RNG.nextInt(25), 70 + RNG.nextInt(20)};
            return new Object[]{rgb, KNNClassifier.Label.PATH};
        } else {
            double r = RNG.nextDouble();
            if (r < 0.70) {
                int[] rgb = {115 + RNG.nextInt(25), 112 + RNG.nextInt(20), 118 + RNG.nextInt(15)};
                return new Object[]{rgb, KNNClassifier.Label.STRUCTURE};
            } else if (r < 0.95) {
                int[] rgb = {60 + RNG.nextInt(20), 105 + RNG.nextInt(25), 45 + RNG.nextInt(15)};
                return new Object[]{rgb, KNNClassifier.Label.VEGETATION};
            } else {
                int[] rgb = {40 + RNG.nextInt(30), 70 + RNG.nextInt(40), 140 + RNG.nextInt(60)};
                return new Object[]{rgb, KNNClassifier.Label.WATER};
            }
        }
    }
}
