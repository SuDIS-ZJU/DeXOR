package algorithms.SZ3.decoder;

import algorithms.Decoder;

public class DoubleSZ3Decoder extends Decoder {
    private static final int DOUBLE_BITS = 64;
    private static final int RADIUS = 32768;

    private double[] decompressedData;
    private int currentIndex;
    private int valueCount;
    private double[] unpredValues;
    private int unpredIndex;

    private double errorBound;
    private double eb2;
    private int unpredCount;
    private boolean initialized;

    public DoubleSZ3Decoder(String inputPath) {
        super(inputPath);
        this.currentIndex = 0;
        this.initialized = false;
    }

    public DoubleSZ3Decoder(String inputPath, String config) {
        super(inputPath, config);
        this.currentIndex = 0;
        this.initialized = false;
    }

    private void initialize() {
        if (initialized) {
            return;
        }

        this.errorBound = in.readDouble(DOUBLE_BITS);
        int radius = (int) in.readLong(32);
        this.unpredCount = (int) in.readLong(32);
        this.valueCount = (int) in.readLong(32);

        this.unpredValues = new double[unpredCount];
        for (int i = 0; i < unpredCount; i++) {
            this.unpredValues[i] = in.readDouble(DOUBLE_BITS);
        }

        this.decompressedData = new double[valueCount];
        this.unpredIndex = 0;
        this.eb2 = 2.0 * errorBound;

        for (int i = 0; i < valueCount; i++) {
            boolean marker = in.readBoolean();

            if (!marker) {
                double value = unpredValues[unpredIndex++];
                decompressedData[i] = value;
            } else {
                int q = (int) in.readLong(16);
                int index = q - RADIUS;
                double decValue = index * eb2;
                decompressedData[i] = decValue;
            }
        }

        this.initialized = true;
        this.currentIndex = 0;
    }

    @Override
    public double decodeDouble() {
        if (!initialized) {
            initialize();
        }

        if (currentIndex >= valueCount) {
            throw new RuntimeException("No more data to decode");
        }

        return decompressedData[currentIndex++];
    }
}