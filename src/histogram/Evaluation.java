package histogram;

import classifier.KNNClassifier;
import navigator.CustomList;

/**
 * Evaluation.java
 * Provides detailed accuracy metrics, confidence analysis, 
 * and a confusion matrix to debug classification errors.
 */
public class Evaluation {

    public static double evaluate(KNNClassifier knn, CustomList<FeatureVector> testData) {
        int total = testData.size();
        if (total == 0) {
            System.out.println("Evaluation Error: No test data provided.");
            return 0.0;
        }

        int correct = 0;
        int numLabels = KNNClassifier.Label.values().length;
        
        // Confusion Matrix: [Actual][Predicted]
        int[][] confusionMatrix = new int[numLabels][numLabels];
        
        double totalConfidenceCorrect = 0;
        double totalConfidenceWrong = 0;

        System.out.println("--- Starting Evaluation on " + total + " samples ---");

        for (int i = 0; i < total; i++) {
            FeatureVector fv = testData.get(i);
            
            // Use the advanced classification method to get confidence scores
            KNNClassifier.ClassificationResult result = knn.classifyWithConfidence(fv.getFeatures());
            
            KNNClassifier.Label actual = stringToLabel(fv.getLabel());
            KNNClassifier.Label predicted = result.getLabel();

            confusionMatrix[actual.ordinal()][predicted.ordinal()]++;

            if (predicted == actual) {
                correct++;
                totalConfidenceCorrect += result.getConfidence();
            } else {
                totalConfidenceWrong += result.getConfidence();
                // Optional: Log specific difficult files
                // System.out.println("  Mismatch in " + fv.getSource() + ": Actual=" + actual + ", Predicted=" + predicted);
            }
        }

        double accuracy = (double) correct / total;
        printSummary(accuracy, correct, total, confusionMatrix, 
                     totalConfidenceCorrect, totalConfidenceWrong);

        return accuracy;
    }

    private static void printSummary(double accuracy, int correct, int total, 
                                     int[][] matrix, double confCorrect, double confWrong) {
        
        System.out.println("\n=========================================");
        System.out.println("       CLASSIFICATION REPORT             ");
        System.out.println("=========================================");
        System.out.printf("Overall Accuracy: %.2f%%\n", accuracy * 100);
        System.out.println("Total Correct:    " + correct + " / " + total);
        
        if (correct > 0) {
            System.out.printf("Avg Confidence (Correct): %.2f%%\n", (confCorrect / correct) * 100);
        }
        if ((total - correct) > 0) {
            System.out.printf("Avg Confidence (Wrong):   %.2f%%\n", (confWrong / (total - correct)) * 100);
        }

        System.out.println("\n--- Confusion Matrix (Rows: Actual, Cols: Predicted) ---");
        KNNClassifier.Label[] labels = KNNClassifier.Label.values();
        
        // Print Header
        System.out.print("           ");
        for (KNNClassifier.Label l : labels) System.out.printf("%-12s", l.name());
        System.out.println();

        // Print Rows
        for (int i = 0; i < matrix.length; i++) {
            System.out.printf("%-10s ", labels[i].name());
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%-12d", matrix[i][j]);
            }
            System.out.println();
        }
        System.out.println("=========================================\n");
    }

    private static KNNClassifier.Label stringToLabel(String s) {
        try {
            return KNNClassifier.Label.valueOf(s.toUpperCase().trim());
        } catch (Exception e) {
            return KNNClassifier.Label.UNKNOWN;
        }
    }
}