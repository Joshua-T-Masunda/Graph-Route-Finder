package histogram;

/**
 * HSVConverter.java
 * Robust conversion from RGB to HSV.
 * Ensures output is strictly within H[0-360], S[0-1], V[0-1].
 */
public class HSVConverter {

    public static float[] rgbToHsv(int r, int g, int b) {
        // Normalize RGB to [0, 1]
        float rPrime = r / 255.0f;
        float gPrime = g / 255.0f;
        float bPrime = b / 255.0f;

        float max = Math.max(rPrime, Math.max(gPrime, bPrime));
        float min = Math.min(rPrime, Math.min(gPrime, bPrime));
        float delta = max - min;

        float h = 0;
        float s = (max == 0) ? 0 : delta / max;
        float v = max;

        // Calculate Hue
        if (delta != 0) {
            if (max == rPrime) {
                h = 60 * (((gPrime - bPrime) / delta));
            } else if (max == gPrime) {
                h = 60 * (((bPrime - rPrime) / delta) + 2);
            } else if (max == bPrime) {
                h = 60 * (((rPrime - gPrime) / delta) + 4);
            }
        }

        // Ensure Hue is within [0, 360]
        if (h < 0) {
            h += 360;
        } else if (h >= 360) {
            h -= 360;
        }

        return new float[]{h, s, v};
    }
}