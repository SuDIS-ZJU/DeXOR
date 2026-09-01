package algorithms.SZ3.encoder;

import algorithms.Encoder;

public class DoubleSZ3Encoder extends Encoder {
    private static final int DOUBLE_BITS = 64;
    private static final int RADIUS = 32768;
    private static final int UNCOMPRESSIBLE = RADIUS * 2;

    private double errorBound;
    private double ebReciprocal;

    private double[] unpredData;
    private int unpredCount;
    private int valueCount;

    private byte[] compressedBuffer;
    private int compressedBitCount;
    private int bufferPos;

    public DoubleSZ3Encoder(String outputPath) {
        super(outputPath);
        this.errorBound = 1e-3;
        this.ebReciprocal = 1.0 / this.errorBound;
        this.unpredData = new double[1024];
        this.unpredCount = 0;
        this.valueCount = 0;
        this.compressedBuffer = new byte[4096];
        this.compressedBitCount = 0;
        this.bufferPos = 0;
    }

    public DoubleSZ3Encoder(String outputPath, String config) {
        super(outputPath, config);
        this.errorBound = 1e-3;
        this.ebReciprocal = 1.0 / this.errorBound;
        this.unpredData = new double[1024];
        this.unpredCount = 0;
        this.valueCount = 0;
        this.compressedBuffer = new byte[4096];
        this.compressedBitCount = 0;
        this.bufferPos = 0;

        if (config != null && !config.isEmpty()) {
            String[] parts = config.split("[,:]");
            for (int i = 0; i < parts.length - 1; i++) {
                String key = parts[i].trim();
                String val = parts[i + 1].trim();
                if (key.equals("eb") || key.equals("error_bound")) {
                    try {
                        this.errorBound = Double.parseDouble(val);
                        this.ebReciprocal = 1.0 / this.errorBound;
                    } catch (NumberFormatException e) {
                    }
                } else if (key.equals("decimal_places")) {
                    try {
                        int dp = Integer.parseInt(val);
                        this.errorBound = Math.pow(10, -dp);
                        this.ebReciprocal = 1.0 / this.errorBound;
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
    }

    @Override
    public int encode(double value) {
        int bitsWritten = 0;
        valueCount++;

        double diff = value;
        boolean negative = diff < 0;
        if (negative) diff = -diff;

        long quantIndex = (long) (diff * ebReciprocal) + 1;

        if (quantIndex < UNCOMPRESSIBLE) {
            quantIndex >>= 1;
            int halfIndex = (int) quantIndex;
            quantIndex <<= 1;

            writeBit(true);
            int shiftedIndex = negative ? (RADIUS - halfIndex) : (RADIUS + halfIndex);
            writeBits(shiftedIndex, 16);
            bitsWritten = 1 + 16;
        } else {
            writeBit(false);
            writeDoubleBits(value);
            bitsWritten = 1 + DOUBLE_BITS;
            addUnpred(value);
        }

        return bitsWritten;
    }

    private void writeBit(boolean b) {
        ensureCapacity(1);
        if (b) {
            compressedBuffer[bufferPos / 8] |= (1 << (7 - (bufferPos % 8)));
        }
        bufferPos++;
        compressedBitCount++;
    }

    private void writeBits(int value, int numBits) {
        ensureCapacity(numBits);
        for (int i = numBits - 1; i >= 0; i--) {
            if ((value & (1 << i)) != 0) {
                compressedBuffer[bufferPos / 8] |= (1 << (7 - (bufferPos % 8)));
            }
            bufferPos++;
        }
        compressedBitCount += numBits;
    }

    private void writeDoubleBits(double value) {
        long bits = Double.doubleToRawLongBits(value);
        writeBits((int) (bits >>> 32), 32);
        writeBits((int) (bits & 0xFFFFFFFF), 32);
    }

    private void ensureCapacity(int additionalBits) {
        while ((bufferPos + additionalBits) > compressedBuffer.length * 8) {
            byte[] newBuffer = new byte[compressedBuffer.length * 2];
            System.arraycopy(compressedBuffer, 0, newBuffer, 0, compressedBuffer.length);
            compressedBuffer = newBuffer;
        }
    }

    private void addUnpred(double value) {
        if (unpredCount >= unpredData.length) {
            double[] newData = new double[unpredData.length * 2];
            System.arraycopy(unpredData, 0, newData, 0, unpredData.length);
            unpredData = newData;
        }
        unpredData[unpredCount++] = value;
    }

    @Override
    public int close() {
        int metadataBits = 0;

        out.write(this.errorBound, DOUBLE_BITS);
        out.write(RADIUS, 32);
        out.write(unpredCount, 32);
        out.write(valueCount, 32);
        metadataBits += DOUBLE_BITS + 96;

        for (int i = 0; i < unpredCount; i++) {
            out.write(unpredData[i], DOUBLE_BITS);
            metadataBits += DOUBLE_BITS;
        }

        int numBytes = (compressedBitCount + 7) / 8;
        byte[] compressed = new byte[numBytes];
        System.arraycopy(compressedBuffer, 0, compressed, 0, numBytes);
        out.writeBytes(compressed);

        out.clear();
        return metadataBits;
    }
}