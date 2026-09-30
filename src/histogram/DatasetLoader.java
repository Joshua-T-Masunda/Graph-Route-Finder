package histogram;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import navigator.CustomList;

public class DatasetLoader {

    public static final int PATCH_SIZE = 16;
    private static final int MAX_PATCHES_PER_IMAGE = 50;
    private static final int MAX_SAMPLES_PER_CLASS = 500;

    private final RichFeatureExtractor extractor;

    public DatasetLoader() {
        this.extractor = new RichFeatureExtractor();
    }

    public CustomList<FeatureVector> loadDataSet(String folder) {
        CustomList<FeatureVector> dataset = new CustomList<>();
        File root = new File(folder);
        File[] labelFolders = root.listFiles();

        if (labelFolders == null) {
            System.out.println("No folders found at: " + folder);
            return dataset;
        }

        for (File labelFolder : labelFolders) {
            if (!labelFolder.isDirectory()) {
                continue;
            }

            String label = labelFolder.getName().toUpperCase();
            File[] images = labelFolder.listFiles();
            if (images == null) {
                continue;
            }

            int classSamples = 0;
            for (File imgFile : images) {
                if (classSamples >= MAX_SAMPLES_PER_CLASS) {
                    break;
                }
                if (!isSupportedImage(imgFile)) {
                    continue;
                }

                try {
                    BufferedImage image = ImageIO.read(imgFile);
                    if (image == null) {
                        System.out.println("Skipping unreadable file: " + imgFile.getName());
                        continue;
                    }
                    classSamples += extractPatchesFromImage(image, label, imgFile.getName(), dataset,
                            MAX_SAMPLES_PER_CLASS - classSamples);
                } catch (IOException ex) {
                    System.err.println("Error loading " + imgFile.getName() + ": " + ex.getMessage());
                }
            }

            System.out.println("Loaded class [" + label + "]: " + classSamples + " patches");
        }

        return dataset;
    }

    public void saveToCSV(CustomList<FeatureVector> dataset, String path) {
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            for (int i = 0; i < dataset.size(); i++) {
                FeatureVector fv = dataset.get(i);
                double[] features = fv.getFeatures();
                for (double feature : features) {
                    out.print(feature);
                    out.print(",");
                }
                out.println(fv.getLabel() + "," + fv.getSource());
            }
            System.out.println("Saved dataset to: " + path);
        } catch (IOException ex) {
            System.err.println("Failed to save dataset: " + ex.getMessage());
        }
    }

    public CustomList<FeatureVector> loadFromCSV(String path) {
        CustomList<FeatureVector> dataset = new CustomList<>();
        File file = new File(path);
        if (!file.exists()) {
            return dataset;
        }

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < extractor.getFeatureDimension() + 2) {
                    continue;
                }

                double[] features = new double[extractor.getFeatureDimension()];
                for (int i = 0; i < features.length; i++) {
                    features[i] = Double.parseDouble(parts[i]);
                }

                dataset.add(new FeatureVector(features, parts[features.length], parts[features.length + 1]));
            }
            System.out.println("Loaded dataset: " + path);
        } catch (Exception ex) {
            System.err.println("Failed to load dataset: " + ex.getMessage());
        }

        return dataset;
    }

    private int extractPatchesFromImage(BufferedImage image, String label, String source,
                                        CustomList<FeatureVector> dataset, int remainingClassSlots) {
        int width = image.getWidth();
        int height = image.getHeight();
        int patchAdded = 0;

        for (int y = 0; y <= height - PATCH_SIZE; y += PATCH_SIZE) {
            for (int x = 0; x <= width - PATCH_SIZE; x += PATCH_SIZE) {
                if (patchAdded >= MAX_PATCHES_PER_IMAGE || patchAdded >= remainingClassSlots) {
                    return patchAdded;
                }

                int[][][] patch = extractPatch(image, x, y);
                double[] features = extractor.extractFromPatch(patch);
                dataset.add(new FeatureVector(features, label, source));
                patchAdded++;
            }
        }

        return patchAdded;
    }

    private int[][][] extractPatch(BufferedImage image, int startX, int startY) {
        int[][][] patch = new int[PATCH_SIZE][PATCH_SIZE][3];
        for (int y = 0; y < PATCH_SIZE; y++) {
            for (int x = 0; x < PATCH_SIZE; x++) {
                int rgb = image.getRGB(startX + x, startY + y);
                patch[y][x][0] = (rgb >> 16) & 0xFF;
                patch[y][x][1] = (rgb >> 8) & 0xFF;
                patch[y][x][2] = rgb & 0xFF;
            }
        }
        return patch;
    }

    private boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }
}
