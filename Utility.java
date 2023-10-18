import java.io.*;
import java.util.*;

public class Utility {

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        // The following is a bad implementation that we have intentionally put in the
        // function to make App.java run, you should
        // write code to reimplement the function without changing any of the input
        // parameters, and making sure the compressed file
        // gets written into outputFileName
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
            System.out.println(oos);
            oos.writeObject(pixels);
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        // The following is a bad implementation that we have intentionally put in the
        // function to make App.java run, you should
        // write code to reimplement the function without changing any of the input
        // parameters, and making sure that it returns
        // an int [][][]
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            Object object = ois.readObject();

            if (object instanceof int[][][]) {
                return (int[][][]) object;
            } else {
                throw new IOException("Invalid object type in the input file");
            }
        }
    }

    // helper functions below

    // 2D wavelet transform
    public static int[][] haarWaveletTransform2D(int[][] input) {
        int numRows = input.length;
        int numCols = input[0].length;

        int[][] output = new int[numRows][numCols];

        // row-wise transformation
        for (int i = 0; i < numRows; i++) {
            output[i] = haarWaveletTransform1D(input[i]);
        }
        // column-wise transformation on the resulting matrix
        for (int j = 0; j < numCols; j++) {
            // the length of the column = numRows
            int[] column = new int[numRows];
            for (int i = 0; i < numRows; i++) {
                // traverse vertically
                column[i] = output[i][j];
            }
            int[] transformedColumn = haarWaveletTransform1D(column);
            for (int i = 0; i < numRows; i++) {
                output[i][j] = transformedColumn[i];
            }
        }

        return output;
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