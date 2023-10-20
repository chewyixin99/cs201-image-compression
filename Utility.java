import java.io.*;
import java.util.*;

public class Utility {

    ContextData contextData;

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {

            // wavelet transform step
            int haarWaveletTransformLevels = 1;
            int[][][] transformedPixels = haarWaveletTransform3D(pixels,
                    haarWaveletTransformLevels);

            // quantization step
            int numColors = 64; // set the number of colors in the palette
            int[][][] quantizedRGB = quantizeColors(transformedPixels, numColors);

            // initialize tree
            HuffmanTree tree = new HuffmanTree();
            tree.buildHuffmanTree(quantizedRGB);

            // encoding step
            String encodedString = tree.encodeQuantizedRGBToString(quantizedRGB);

            // final step, package necessary data to decode
            int numRows = quantizedRGB.length;
            int numCols = quantizedRGB[0].length;
            int numChannels = quantizedRGB[0][0].length;
            ContextData data = new ContextData(tree, encodedString, numRows, numCols, numChannels);
            this.contextData = data;

            // write to outputstream and save the file
            oos.writeObject(encodedString);
        }
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            Object object = ois.readObject();
            if (object instanceof String) {
                // retrieve huffman tree
                HuffmanTree tree = contextData.getHuffmanTree();
                String encodedString = (String) object;
                // decoding step
                int[][][] decodedQuantizedRGB = tree.decodeStringToQuantizedRGB(encodedString,
                        contextData.getNumRows(), contextData.getNumCols(), contextData
                                .getNumChannels());

                // inverse transform step
                int haarWaveletTransformLevels = 1;

                // int[][][] test = inverseHaarWaveletTransform3D(decodedQuantizedRGB,
                // haarWaveletTransformLevels);
                // System.out.println(test.length + " " + test[0].length + " " +
                // test[0][0].length);

                return inverseHaarWaveletTransform3D(decodedQuantizedRGB,
                        haarWaveletTransformLevels);
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

    public static Map<List<Integer>, Integer> calculateRGBFrequencies(int[][][] quantizedRGB) {
        HashMap<List<Integer>, Integer> rgbFrequencies = new HashMap<>();

        int numRows = quantizedRGB.length;
        int numCols = quantizedRGB[0].length;

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                List<Integer> currKey = Arrays.stream(quantizedRGB[i][j]).boxed().toList();
                if (rgbFrequencies.containsKey(currKey)) {
                    Integer currVal = rgbFrequencies.get(currKey);
                    rgbFrequencies.put(currKey, currVal + 1);
                } else {
                    rgbFrequencies.put(currKey, 1);
                }
            }
        }

        return rgbFrequencies;
    }

}

class HuffmanNode implements Comparable<HuffmanNode>, Serializable {
    int frequency;
    List<Integer> value;
    HuffmanNode left;
    HuffmanNode right;

    public HuffmanNode(List<Integer> value, int frequency) {
        this.value = value;
        this.frequency = frequency;
        left = null;
        right = null;
    }

    @Override
    public int compareTo(HuffmanNode other) {
        return this.frequency - other.frequency;
    }

    public boolean equals(HuffmanNode other) {
        return this.frequency == other.frequency;
    }
}

class HuffmanTree implements Serializable {

    private Map<List<Integer>, String> huffmanCodes;
    private HuffmanNode rootNode;

    public HuffmanTree() {

    }

    public Map<List<Integer>, String> getHuffmanCodes() {
        return this.huffmanCodes;
    }

    public HuffmanNode getRootNode() {
        return this.rootNode;
    }

    public void buildHuffmanTree(int[][][] quantizedRGB) {
        PriorityQueue<HuffmanNode> priorityQueue = new PriorityQueue<>();
        Map<List<Integer>, String> huffmanCodes = new HashMap<>();

        Map<List<Integer>, Integer> rgbFrequencies = calculateRGBFrequencies(quantizedRGB);

        // Create a leaf node for each unique RGB value and add it to the priority
        // queue.
        for (Map.Entry<List<Integer>, Integer> entry : rgbFrequencies.entrySet()) {
            HuffmanNode node = new HuffmanNode(entry.getKey(), entry.getValue());
            priorityQueue.add(node);
        }

        // Build the Huffman tree
        while (priorityQueue.size() > 1) {
            HuffmanNode left = priorityQueue.poll();
            HuffmanNode right = priorityQueue.poll();

            HuffmanNode parent = new HuffmanNode(null, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;

            priorityQueue.add(parent);
        }

        // Last remaining element becomes the root
        if (!priorityQueue.isEmpty()) {
            HuffmanNode root = priorityQueue.peek();
            generateHuffmanCodes(root, "", huffmanCodes);
        }

        this.huffmanCodes = huffmanCodes;
        this.rootNode = priorityQueue.peek();
    }

    private static void generateHuffmanCodes(HuffmanNode node, String code, Map<List<Integer>, String> huffmanCodes) {
        if (node == null) {
            return;
        }

        if (node.value != null) {
            huffmanCodes.put(node.value, code);
        }

        generateHuffmanCodes(node.left, code + "0", huffmanCodes);
        generateHuffmanCodes(node.right, code + "1", huffmanCodes);
    }

    public String encodeQuantizedRGBToString(int[][][] quantizedRGB) {
        int numRows = quantizedRGB.length;
        int numCols = quantizedRGB[0].length;
        StringBuilder encodedString = new StringBuilder();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                int[] rgbArr = quantizedRGB[i][j];
                List<Integer> rgbList = Arrays.stream(rgbArr).boxed().toList();
                String code = this.huffmanCodes.get(rgbList);
                encodedString.append(code);
            }
        }
        return encodedString.toString();
    }

    public int[][][] decodeStringToQuantizedRGB(String encodedString, int numRows, int numCols, int numChannels) {
        int[][][] decodedArr = new int[numRows][numCols][numChannels];
        HuffmanNode currentNode = rootNode;
        int rowIdx = 0;
        int colIdx = 0;

        for (char bit : encodedString.toCharArray()) {
            if (bit == '0') {
                currentNode = currentNode.left;
            } else if (bit == '1') {
                currentNode = currentNode.right;
            }

            if (currentNode.value != null) {
                int[] valArr = currentNode.value.stream().mapToInt(Integer::intValue).toArray();
                decodedArr[rowIdx][colIdx] = valArr;
                colIdx = colIdx + 1;
                if (colIdx == numCols) {
                    colIdx = 0;
                    rowIdx = rowIdx + 1;
                }
                currentNode = rootNode;
            }
        }

        return decodedArr;
    }

    public static Map<List<Integer>, Integer> calculateRGBFrequencies(int[][][] quantizedRGB) {
        HashMap<List<Integer>, Integer> rgbFrequencies = new HashMap<>();

        int numRows = quantizedRGB.length;
        int numCols = quantizedRGB[0].length;

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                List<Integer> currKey = Arrays.stream(quantizedRGB[i][j]).boxed().toList();
                if (rgbFrequencies.containsKey(currKey)) {
                    Integer currVal = rgbFrequencies.get(currKey);
                    rgbFrequencies.put(currKey, currVal + 1);
                } else {
                    rgbFrequencies.put(currKey, 1);
                }
            }
        }

        return rgbFrequencies;
    }
}

class ContextData implements Serializable {
    private HuffmanTree huffmanTree;
    private String encodedString;
    private int numRows;
    private int numCols;
    private int numChannels;

    public ContextData(HuffmanTree huffmanTree, String encodedString, int numRows, int numCols,
            int numChannels) {
        this.huffmanTree = huffmanTree;
        this.encodedString = encodedString;
        this.numRows = numRows;
        this.numCols = numCols;
        this.numChannels = numChannels;
    }

    public HuffmanTree getHuffmanTree() {
        return this.huffmanTree;
    }

    public String getEncodedString() {
        return this.encodedString;
    }

    public int getNumRows() {
        return this.numRows;
    }

    public int getNumCols() {
        return this.numCols;
    }

    public int getNumChannels() {
        return this.numChannels;
    }

}
