import java.io.*;
import java.util.*;

public class Utility {

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
            // manipulate pixels (or a copy of) before writing to outputstream
            // todo: edit here

            // wavelet transform step
            int haarWaveletTransformLevels = 1;
            int[][][] transformedPixels = haarWaveletTransform3D(pixels,
                    haarWaveletTransformLevels);

            // quantization step
            int numColors = 64; // set the number of colors in the palette
            int[][][] quantizedRGB = quantizeColors(transformedPixels, numColors);

            // encoding step
            List<Integer> encodedQuantizedRGB = encodeSymbols(quantizedRGB);
            // todo: end edit here
            // write to outputstream and save the file
            oos.writeObject(quantizedRGB);
            // oos.writeObject(encodedQuantizedRGB);
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            Object object = ois.readObject();

            if (object instanceof List) {
                // reverse the compression algorithm before returning the original object
                // manipulate object (or a copy of) before returning
                // todo: edit here
                // decoding step
                List<Integer> encodedQuantizedRGB = convertToIntegerList(Arrays.asList(object));
                int[][][] decodedQuantizedRGB = decodeSymbols(encodedQuantizedRGB);

                // inverse wavelet transform step
                int haarWaveletTransformLevels = 1;
                int[][][] outputPixels = inverseHaarWaveletTransform3D(decodedQuantizedRGB, haarWaveletTransformLevels);
                // todo: end edit here
                return outputPixels;
            } else if (object instanceof int[][][]) {
                // todo: delete this block once decoding is implemented
                int haarWaveletTransformLevels = 1;
                int[][][] outputPixels = inverseHaarWaveletTransform3D((int[][][]) object, haarWaveletTransformLevels);
                // todo: end edit here
                return outputPixels;
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
            int diffIndex = 2 * i + 1;
            output[i] = (input[sumIndex] + input[diffIndex]) / 2;
            output[i + length / 2] = (input[sumIndex] - input[diffIndex]) / 2;
        }

        return output;
    }

    // 3D inverse wavelet transform
    public static int[][][] inverseHaarWaveletTransform3D(int[][][] image, int levels) {
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
                    int[] transformedRow = inverseHaarWaveletTransform1D(row);
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

    // 1D inverse wavelet transform
    public static int[] inverseHaarWaveletTransform1D(int[] input) {
        int length = input.length;
        int[] output = new int[length];

        for (int i = 0; i < length / 2; i++) {
            int sumIndex = 2 * i;
            int diffIndex = 2 * i + 1;
            output[sumIndex] = input[i] + input[i + length / 2];
            output[diffIndex] = input[i] - input[i + length / 2];
        }

        return output;
    }

    // quantization step
    public static int[][][] quantizeColors(int[][][] rgbArray, int numColors) {
        int numRows = rgbArray.length;
        int numCols = rgbArray[0].length;
        int[][][] quantizedRGB = new int[numRows][numCols][3];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                int red = quantizeChannel(rgbArray[i][j][0], numColors);
                int green = quantizeChannel(rgbArray[i][j][1], numColors);
                int blue = quantizeChannel(rgbArray[i][j][2], numColors);
                quantizedRGB[i][j][0] = red;
                quantizedRGB[i][j][1] = green;
                quantizedRGB[i][j][2] = blue;
            }
        }

        return quantizedRGB;
    }

    public static int quantizeChannel(int value, int numColors) {
        int step = 256 / numColors;
        return (value / step) * step;
    }

    public static List<Integer> encodeSymbols(int[][][] quantizedRGB) {
        int numRows = quantizedRGB.length;
        int numCols = quantizedRGB[0].length;
        int numChannels = quantizedRGB[0][0].length;
        List<Integer> encodedSymbols = new ArrayList<>();
        AdaptiveHuffmanTree tree = new AdaptiveHuffmanTree();

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                for (int k = 0; k < numChannels; k++) {
                    int symbol = quantizedRGB[i][j][k];
                    List<Integer> bits = tree.encodeSymbol(symbol);
                    encodedSymbols.addAll(bits);
                }
            }
        }

        return encodedSymbols;
    }

    public static int[][][] decodeSymbols(List<Integer> encodedSymbols) {
        int numChannels = 3; // Assuming 3 color channels (R, G, B)
        List<Integer> decodedSymbols = new ArrayList<>();
        int symbol;
        AdaptiveHuffmanTree tree = new AdaptiveHuffmanTree();

        int row = 0;
        int col = 0;
        int channel = 0;

        while (!encodedSymbols.isEmpty()) {
            symbol = tree.decodeSymbol(encodedSymbols);
            decodedSymbols.add(symbol);

            if (channel == numChannels - 1) {
                channel = 0;
                col++;
                if (col == encodedSymbols.size()) {
                    col = 0;
                    row++;
                }
            } else {
                channel++;
            }
        }

        int numRows = row + 1;
        int numCols = col + 1;
        int[][][] decodedRGB = new int[numRows][numCols][numChannels];

        for (int i = 0; i < decodedSymbols.size(); i++) {
            int value = decodedSymbols.get(i);
            int yi = i / (numCols * numChannels);
            int xi = (i / numChannels) % numCols;
            int ci = i % numChannels;
            decodedRGB[yi][xi][ci] = value;
        }

        return decodedRGB;
    }

    public static List<Integer> convertToIntegerList(List<Object> inputList) {
        List<Integer> output = new ArrayList<>();
        for (Object item : inputList) {
            output.add((Integer) item);
        }
        return output;
    }

}

class AdaptiveHuffmanTree {
    private AdaptiveHuffmanNode root;
    private AdaptiveHuffmanNode NYT; // Not Yet Transmitted node

    public AdaptiveHuffmanTree() {
        root = new AdaptiveHuffmanNode();
        NYT = root;
    }

    public List<Integer> encodeSymbol(int symbol) {
        List<Integer> bits = new ArrayList<>();
        AdaptiveHuffmanNode node = findNodeBySymbol(symbol);

        if (node == null) {
            bits.addAll(encodeSymbol(NYT, symbol));
            updateTree(symbol);
        } else {
            bits.addAll(encodeSymbol(node, symbol));
            updateTree(node);
        }

        return bits;
    }

    public int decodeSymbol(List<Integer> bits) {
        AdaptiveHuffmanNode node = root;

        while (true) {
            if (node.isLeaf()) {
                return node.symbol;
            }

            int bit = bits.remove(0);
            node = (bit == 0) ? node.left : node.right;
        }
    }

    private List<Integer> encodeSymbol(AdaptiveHuffmanNode node, int symbol) {
        List<Integer> bits = new ArrayList<>();

        while (node != root) {
            if (node.isNYT()) {
                bits.add(0); // 0 represents NYT
                bits.addAll(toBinary(symbol, 8)); // Assuming 8-bit symbols
                return bits;
            }

            if (node.isLeftChild()) {
                bits.add(0);
            } else {
                bits.add(1);
            }

            node = node.parent;
        }

        return bits;
    }

    private List<Integer> toBinary(int value, int numBits) {
        List<Integer> bits = new ArrayList<>();
        for (int i = 0; i < numBits; i++) {
            bits.add((value >> (numBits - i - 1)) & 1);
        }
        return bits;
    }

    // todo: implement the following 3 methods for encoding and decoding

    private AdaptiveHuffmanNode findNodeBySymbol(int symbol) {
        // Implement search for the node containing the symbol
        // Return null if not found
        // You may need to traverse the tree
        return null;
    }

    private void updateTree(int symbol) {
        // Implement tree update logic to accommodate the new symbol
        // You will need to adjust node weights and tree structure
    }

    private void updateTree(AdaptiveHuffmanNode node) {
        // Implement tree update logic to accommodate the recently transmitted node
        // You will need to adjust node weights and tree structure
    }
}

class AdaptiveHuffmanNode {
    int weight;
    int symbol;
    AdaptiveHuffmanNode parent;
    AdaptiveHuffmanNode left;
    AdaptiveHuffmanNode right;

    public AdaptiveHuffmanNode() {
        weight = 0;
        symbol = -1;
        parent = null;
        left = null;
        right = null;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public boolean isNYT() {
        return symbol == -1;
    }

    public boolean isLeftChild() {
        return parent != null && parent.left == this;
    }
}
