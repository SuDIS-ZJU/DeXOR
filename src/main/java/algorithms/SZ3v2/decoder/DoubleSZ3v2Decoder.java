package algorithms.SZ3v2.decoder;

import algorithms.Decoder;

public class DoubleSZ3v2Decoder extends Decoder {
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
    private double previousValue;

    public DoubleSZ3v2Decoder(String inputPath) {
        super(inputPath);
        this.currentIndex = 0;
        this.initialized = false;
    }

    public DoubleSZ3v2Decoder(String inputPath, String config) {
        super(inputPath, config);
        this.currentIndex = 0;
        this.initialized = false;
    }

    private void initialize() {
        if (initialized) {
            return;
        }

        try {
            byte[] allBytes = in.readAllBytes();
            
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(allBytes);
            java.io.DataInputStream dis = new java.io.DataInputStream(bais);

            this.errorBound = dis.readDouble();
            dis.readInt();
            this.unpredCount = dis.readInt();
            this.valueCount = dis.readInt();

            this.unpredValues = new double[unpredCount];
            for (int i = 0; i < unpredCount; i++) {
                this.unpredValues[i] = dis.readDouble();
            }

            int metadataBytes = 8 + 4 + 4 + 4 + (unpredCount * 8);
            byte[] compressedData = new byte[allBytes.length - metadataBytes];
            System.arraycopy(allBytes, metadataBytes, compressedData, 0, compressedData.length);

            this.decompressedData = new double[valueCount];
            this.unpredIndex = 0;
            this.eb2 = 2.0 * errorBound;
            this.previousValue = 0;

            BitReader reader = new BitReader(compressedData);

            for (int i = 0; i < valueCount; i++) {
                boolean marker = reader.readBit();

                if (!marker) {
                    long bits = reader.readBits(64);
                    double value = Double.longBitsToDouble(bits);
                    decompressedData[i] = value;
                    previousValue = value;
                } else {
                    int q = (int) reader.readBits(16);
                    int index = q - RADIUS;
                    double decValue = previousValue + index * eb2;
                    decompressedData[i] = decValue;
                    previousValue = decValue;
                }
            }

            this.initialized = true;
            this.currentIndex = 0;
        } catch (Exception e) {
            e.printStackTrace();
            this.decompressedData = new double[0];
            this.valueCount = 0;
            this.initialized = true;
        }
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

    private static class BitReader {
        private final byte[] data;
        private int bitPosition;
        private final int dataBits;

        public BitReader(byte[] data) {
            this.data = data;
            this.bitPosition = 0;
            this.dataBits = data.length * 8;
        }

        public boolean readBit() {
            if (bitPosition >= dataBits) {
                return false;
            }
            int byteIndex = bitPosition / 8;
            int bitIndex = 7 - (bitPosition % 8);
            bitPosition++;
            return (data[byteIndex] & (1 << bitIndex)) != 0;
        }

        public long readBits(int numBits) {
            long result = 0;
            for (int i = numBits - 1; i >= 0; i--) {
                if (bitPosition >= dataBits) {
                    break;
                }
                int byteIndex = bitPosition / 8;
                int bitIndex = 7 - (bitPosition % 8);
                if ((data[byteIndex] & (1 << bitIndex)) != 0) {
                    result |= (1L << i);
                }
                bitPosition++;
            }
            return result;
        }
    }
}