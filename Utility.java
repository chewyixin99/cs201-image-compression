import java.io.*;
import java.util.*;

public class Utility {

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
            // manipulate pixels (or a copy of) before writing to outputstream
            // todo: edit here
            int haarWaveletTransformLevels = 1;
            int[][][] outputPixels = haarWaveletTransform3D(pixels,
                    haarWaveletTransformLevels);
            // todo: end edit here
            // write to outputstream and save the file
            oos.writeObject(outputPixels);
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            Object object = ois.readObject();
            if (object instanceof int[][][]) {
                // reverse the compression algorithm before returning the original object
                // manipulate object (or a copy of) before returning
                // todo: edit here

                // todo: end edit here
                return (int[][][]) object;
            } else {
                throw new IOException("Invalid object type in the input file");
            }
        }
    }

    // helper functions below

    // 3D wavelet transform
    public static int[][][] haarWaveletTransform3D(int[][][] image, int levels) {
        int numRows = image.length;
        int numCols = image[0].length;
        int depth = image[0][0].length; // Number of color channels (e.g., R, G, B)

        int[][][] outputImage = new int[numRows][numCols][depth];

        for (int level = 0; level < levels; level++) {
            // Apply Haar wavelet transform to each color channel
            for (int d = 0; d < depth; d++) {
                for (int i = 0; i < numRows; i++) {
                    // create a new row to contain all of 1 color channel
                    int[] row = new int[numCols];
                    for (int j = 0; j < numCols; j++) {
                        row[j] = image[i][j][d];
                    }
                    // apply wavelet transform
                    int[] transformedRow = haarWaveletTransform1D(row);
                    // write to output
                    for (int j = 0; j < numCols; j++) {
                        outputImage[i][j][d] = transformedRow[j];
                    }
                }
            }
            // Update dimensions for the next level
            numRows /= 2;
            numCols /= 2;
        }

        return outputImage;
    }

    // 1D wavelet transform
    public static int[] haarWaveletTransform1D(int[] input) {
        int length = input.length;
        int[] output = new int[length];

        for (int i = 0; i < length / 2; i++) {
            int sumIndex = 2 * i;
            int diffIndex = 2 * 1 + 1;
            output[i] = (input[sumIndex] + input[diffIndex]) / 2;
            output[i + length / 2] = (input[sumIndex] - input[diffIndex]) / 2;
        }

        return output;
    }

}