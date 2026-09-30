package classifier;

import histogram.DatasetLoader;
import histogram.FeatureVector;
import navigator.CustomList;

public class DatasetPreprocessor {

    public static void main(String[] args) {
        String datasetFolder = args.length > 0 ? args[0] : "data";
        String outputCSV = args.length > 1 ? args[1] : "persistent_training.csv";

        DatasetLoader loader = new DatasetLoader();
        System.out.println("Loading dataset from folder: " + datasetFolder);

        CustomList<FeatureVector> dataset = loader.loadDataSet(datasetFolder);
        System.out.println("Extracted samples: " + dataset.size());

        loader.saveToCSV(dataset, outputCSV);
        System.out.println("Preprocessing complete.");
    }
}
