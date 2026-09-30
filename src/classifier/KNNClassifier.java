package classifier;

import histogram.DatasetLoader;
import histogram.DistanceMetric;
import histogram.FeatureVector;
import histogram.RichFeatureExtractor;
import navigator.CustomList;
import navigator.GraphNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 * Downstream terrain classifier used after handwritten graph construction and
 * feature extraction have already been completed.
 *
 * <p>The class name is kept for compatibility with earlier project code. The
 * implementation uses Weka's RandomForest only for the downstream classification
 * task, which keeps graph construction and feature engineering inside the
 * student's Java code.</p>
 */
public class KNNClassifier {

    private static final String CSV_PATH = "persistent_training.csv";
    private static final boolean USE_WEKA = Boolean.getBoolean("miniProject.useWeka");

    /**
     * Terrain labels used for classification and pathfinding weights.
     */
    public enum Label {
        PATH, STRUCTURE, VEGETATION, WATER, ROAD, POTHOLE, UNKNOWN;

        /**
         * Returns whether this terrain can be traversed by route algorithms.
         */
        public boolean isWalkable() {
            return this == PATH || this == ROAD;
        }

        /**
         * Returns the movement cost used by shortest-path algorithms.
         */
        public double getWeight() {
            switch (this) {
                case ROAD: return 0.8;
                case PATH: return 1.0;
                case POTHOLE: return 5.0;
                case VEGETATION: return 15.0;
                case STRUCTURE:
                case WATER: return Double.POSITIVE_INFINITY;
                default: return 10.0;
            }
        }

        /**
         * Returns a display name for the JavaFX interface.
         */
        public String getDisplayName() {
            switch (this) {
                case ROAD: return "Highway/Road";
                case PATH: return "Footpath/Trail";
                case POTHOLE: return "Pothole";
                case STRUCTURE: return "Building/House";
                case VEGETATION: return "Trees/Foliage";
                case WATER: return "Water/Mud";
                default: return "Unknown";
            }
        }
    }

    private final int k;
    private final CustomList<FeatureVector> trainingData;
    private final RichFeatureExtractor featureExtractor;

    private RandomForest randomForest;
    private Instances wekaDatasetStructure;
    private boolean isModelBuilt;
    private boolean useFallbackKnn;

    /**
     * Creates the classifier and loads any persisted training data.
     *
     * @param k retained for compatibility with earlier k-NN versions
     */
    public KNNClassifier(int k) {
        this.k = k;
        this.trainingData = new CustomList<>();
        this.featureExtractor = new RichFeatureExtractor();
        setupWekaStructure();
        loadFromCSV();
        buildWekaModel();
    }

    private void setupWekaStructure() {
        ArrayList<Attribute> attributes = new ArrayList<>();

        for (int i = 0; i < featureExtractor.getFeatureDimension(); i++) {
            attributes.add(new Attribute("feature_" + i));
        }

        ArrayList<String> classValues = new ArrayList<>();
        for (Label label : Label.values()) {
            classValues.add(label.name());
        }
        attributes.add(new Attribute("classLabel", classValues));

        wekaDatasetStructure = new Instances("HazardDataset", attributes, 0);
        wekaDatasetStructure.setClassIndex(wekaDatasetStructure.numAttributes() - 1);
    }

    /**
     * Builds a Weka dataset from the custom training list and trains the model.
     */
    private void buildWekaModel() {
        if (trainingData.size() == 0) {
            isModelBuilt = false;
            return;
        }

        if (!USE_WEKA) {
            isModelBuilt = false;
            useFallbackKnn = true;
            return;
        }

        try {
            Instances trainingInstances = new Instances(wekaDatasetStructure, trainingData.size());

            for (int i = 0; i < trainingData.size(); i++) {
                FeatureVector featureVector = trainingData.get(i);
                double[] features = featureVector.getFeatures();
                if (features == null || features.length != featureExtractor.getFeatureDimension()) {
                    continue;
                }

                DenseInstance instance = new DenseInstance(wekaDatasetStructure.numAttributes());
                instance.setDataset(wekaDatasetStructure);

                for (int j = 0; j < features.length; j++) {
                    instance.setValue(j, features[j]);
                }
                instance.setValue(wekaDatasetStructure.numAttributes() - 1, featureVector.getLabel());
                trainingInstances.add(instance);
            }

            if (trainingInstances.numInstances() == 0) {
                isModelBuilt = false;
                System.err.println("No valid training instances were available.");
                return;
            }

            randomForest = new RandomForest();
            randomForest.setNumIterations(100);
            randomForest.buildClassifier(trainingInstances);
            isModelBuilt = true;
        } catch (Throwable ex) {
            isModelBuilt = false;
            useFallbackKnn = trainingData.size() > 0;
            System.err.println("Weka model unavailable; using handwritten k-NN fallback. Reason: " + ex.getMessage());
        }
    }

    /**
     * Classifies a feature vector and returns only the predicted label.
     *
     * @param inFeatures handwritten feature vector
     * @return predicted terrain label
     */
    public Label classify(double[] inFeatures) {
        return classifyWithConfidence(inFeatures).getLabel();
    }

    /**
     * Classifies a feature vector and returns label plus confidence.
     *
     * @param inFeatures handwritten feature vector
     * @return classification result
     */
    public ClassificationResult classifyWithConfidence(double[] inFeatures) {
        if (trainingData.size() == 0 || inFeatures == null) {
            return new ClassificationResult(Label.UNKNOWN, 0.0);
        }

        if (!isModelBuilt || useFallbackKnn) {
            return classifyWithFallbackKnn(inFeatures);
        }

        try {
            DenseInstance target = new DenseInstance(wekaDatasetStructure.numAttributes());
            target.setDataset(wekaDatasetStructure);

            int featureCount = Math.min(inFeatures.length, featureExtractor.getFeatureDimension());
            for (int i = 0; i < featureCount; i++) {
                target.setValue(i, inFeatures[i]);
            }

            double classIndex = randomForest.classifyInstance(target);
            double[] distribution = randomForest.distributionForInstance(target);
            Label predictedLabel = Label.values()[(int) classIndex];
            double confidence = distribution[(int) classIndex];

            return new ClassificationResult(predictedLabel, confidence);
        } catch (Throwable ex) {
            System.err.println("Weka classification failed; using handwritten k-NN fallback. Reason: " + ex.getMessage());
            useFallbackKnn = true;
            return classifyWithFallbackKnn(inFeatures);
        }
    }

    private ClassificationResult classifyWithFallbackKnn(double[] inFeatures) {
        int labelCount = Label.values().length;
        int[] votes = new int[labelCount];
        double[] confidenceWeight = new double[labelCount];
        double[] bestDistances = new double[Math.max(1, k)];
        Label[] bestLabels = new Label[bestDistances.length];

        for (int i = 0; i < bestDistances.length; i++) {
            bestDistances[i] = Double.MAX_VALUE;
        }

        for (int i = 0; i < trainingData.size(); i++) {
            FeatureVector featureVector = trainingData.get(i);
            double distance = DistanceMetric.calculateDistance(inFeatures, featureVector.getFeatures());
            int worstIndex = findWorstBestDistance(bestDistances);

            if (distance < bestDistances[worstIndex]) {
                bestDistances[worstIndex] = distance;
                bestLabels[worstIndex] = parseLabel(featureVector.getLabel());
            }
        }

        int used = 0;
        for (int i = 0; i < bestLabels.length; i++) {
            if (bestLabels[i] != null) {
                int ordinal = bestLabels[i].ordinal();
                votes[ordinal]++;
                confidenceWeight[ordinal] += 1.0 / (1.0 + bestDistances[i]);
                used++;
            }
        }

        if (used == 0) {
            return new ClassificationResult(Label.UNKNOWN, 0.0);
        }

        Label bestLabel = Label.UNKNOWN;
        int bestVotes = -1;
        double bestWeight = -1.0;
        for (Label label : Label.values()) {
            int ordinal = label.ordinal();
            if (votes[ordinal] > bestVotes
                    || (votes[ordinal] == bestVotes && confidenceWeight[ordinal] > bestWeight)) {
                bestVotes = votes[ordinal];
                bestWeight = confidenceWeight[ordinal];
                bestLabel = label;
            }
        }

        return new ClassificationResult(bestLabel, (double) bestVotes / used);
    }

    private int findWorstBestDistance(double[] bestDistances) {
        int worstIndex = 0;
        for (int i = 1; i < bestDistances.length; i++) {
            if (bestDistances[i] > bestDistances[worstIndex]) {
                worstIndex = i;
            }
        }
        return worstIndex;
    }

    private Label parseLabel(String label) {
        try {
            return Label.valueOf(label);
        } catch (Exception ex) {
            return Label.UNKNOWN;
        }
    }

    /**
     * Adds one manually labelled graph node to the persisted training set.
     */
    public void addTrainingSample(GraphNode node, Label label, String source) {
        if (node == null || label == null) {
            return;
        }

        double[] features = node.featureVector;
        if (features == null || features.length != featureExtractor.getFeatureDimension()) {
            features = featureExtractor.extractFromNode(node);
            node.setFeatureVector(features);
        }

        FeatureVector featureVector = new FeatureVector(features, label.name(), source);
        trainingData.add(featureVector);
        saveToCSV(featureVector);
        buildWekaModel();
    }

    /**
     * Replaces training data and saves the replacement to CSV.
     */
    public void setTrainingData(CustomList<FeatureVector> data) {
        trainingData.clear();
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                trainingData.add(data.get(i));
            }
        }
        saveAllToCSV();
        buildWekaModel();
    }

    /**
     * Replaces training data for this runtime only.
     */
    public void setRuntimeTrainingData(CustomList<FeatureVector> data) {
        trainingData.clear();
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                trainingData.add(data.get(i));
            }
        }
        buildWekaModel();
    }

    /**
     * Clears persisted and in-memory training samples.
     */
    public void clearTrainingData() {
        trainingData.clear();
        isModelBuilt = false;
        File csvFile = new File(CSV_PATH);
        if (csvFile.exists() && !csvFile.delete()) {
            System.err.println("Could not delete training file: " + CSV_PATH);
        }
    }

    private void saveToCSV(FeatureVector featureVector) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(CSV_PATH, true)))) {
            writeFeatureVector(out, featureVector);
        } catch (IOException ex) {
            System.err.println("Failed to save training sample: " + ex.getMessage());
        }
    }

    /**
     * Loads persisted training samples from CSV.
     */
    public void loadFromCSV() {
        File file = new File(CSV_PATH);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                FeatureVector featureVector = parseFeatureVector(line);
                if (featureVector != null) {
                    trainingData.add(featureVector);
                }
            }
        } catch (IOException ex) {
            System.err.println("CSV load error: " + ex.getMessage());
        }
    }

    /**
     * Saves all current training samples to CSV.
     */
    public void saveAllToCSV() {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(CSV_PATH, false)))) {
            for (int i = 0; i < trainingData.size(); i++) {
                writeFeatureVector(out, trainingData.get(i));
            }
        } catch (IOException ex) {
            System.err.println("Failed to save all training samples: " + ex.getMessage());
        }
    }

    /**
     * Returns the number of training samples currently loaded.
     */
    public int getTrainingSize() {
        return trainingData.size();
    }

    /**
     * Returns the retained neighbour count setting from earlier k-NN versions.
     */
    public int getK() {
        return k;
    }

    /**
     * Returns the handwritten feature extractor used before classification.
     */
    public RichFeatureExtractor getFeatureExtractor() {
        return featureExtractor;
    }

    /**
     * Returns the in-memory training data.
     */
    public CustomList<FeatureVector> getTrainingData() {
        return trainingData;
    }

    /**
     * Loads labelled image folders and adds the extracted samples to training.
     */
    public int loadTrainingFromFolder(String folderPath) {
        DatasetLoader loader = new DatasetLoader();
        CustomList<FeatureVector> data = loader.loadDataSet(folderPath);
        int before = trainingData.size();

        for (int i = 0; i < data.size(); i++) {
            trainingData.add(data.get(i));
        }

        saveAllToCSV();
        buildWekaModel();
        return trainingData.size() - before;
    }

    private FeatureVector parseFeatureVector(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split(",", -1);
        int expectedLength = featureExtractor.getFeatureDimension();
        if (parts.length < expectedLength + 2) {
            return null;
        }

        double[] features = new double[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            try {
                features[i] = Double.parseDouble(parts[i]);
            } catch (NumberFormatException ex) {
                features[i] = 0.0;
            }
        }

        return new FeatureVector(features, parts[expectedLength], parts[expectedLength + 1]);
    }

    private void writeFeatureVector(PrintWriter out, FeatureVector featureVector) {
        double[] features = featureVector.getFeatures();
        for (int i = 0; i < features.length; i++) {
            out.print(features[i]);
            out.print(",");
        }
        out.println(featureVector.getLabel() + "," + featureVector.getSource());
    }

    /**
     * Immutable classification output used by graph nodes and UI code.
     */
    public static class ClassificationResult {
        private final Label label;
        private final double confidence;

        /**
         * Creates a result with label and confidence.
         */
        public ClassificationResult(Label label, double confidence) {
            this.label = label == null ? Label.UNKNOWN : label;
            this.confidence = confidence;
        }

        /**
         * Returns the predicted label.
         */
        public Label getLabel() {
            return label;
        }

        /**
         * Returns confidence from 0.0 to 1.0.
         */
        public double getConfidence() {
            return confidence;
        }

        @Override
        public String toString() {
            return label.getDisplayName() + " (" + String.format("%.1f", confidence * 100) + "%)";
        }
    }
}
