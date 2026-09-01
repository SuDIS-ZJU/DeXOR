package algorithms.SZ3v2.encoder;

import algorithms.Encoder;

import java.io.DataOutputStream;
import java.io.FileOutputStream;

public class DoubleSZ3v2Encoder extends Encoder {
    private static final int DOUBLE_BITS = 64;
    private static final int RADIUS = 32768;
    private static final int UNCOMPRESSIBLE = RADIUS * 2;

    private double errorBound;
    private double ebReciprocal;

    private double[] unpredData;
    private int unpredCount;
    private int valueCount;

    private byte[] bitBuffer;
    private int bitPosition;
    private int bufferCapacity;

    private final String outputPath;
    private double previousValue;
    private boolean firstValue;

    public DoubleSZ3v2Encoder(String outputPath) {
        super(outputPath);
        this.outputPath = outputPath;
        this.errorBound = 1e-3;
        this.ebReciprocal = 1.0 / this.errorBound;
        this.unpredData = new double[1024];
        this.unpredCount = 0;
        this.valueCount = 0;
        this.bufferCapacity = 4096;
        this.bitBuffer = new byte[bufferCapacity];
        this.bitPosition = 0;
        this.previousValue = 0;
        this.firstValue = true;
    }

    public DoubleSZ3v2Encoder(String outputPath, String config) {
        super(outputPath, config);
        this.outputPath = outputPath;
        this.errorBound = 1e-3;
        this.ebReciprocal = 1.0 / this.errorBound;
        this.unpredData = new double[1024];
        this.unpredCount = 0;
        this.valueCount = 0;
        this.bufferCapacity = 4096;
        this.bitBuffer = new byte[bufferCapacity];
        this.bitPosition = 0;
        this.previousValue = 0;
        this.firstValue = true;

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
                } else if (key.equals("failed_decimal_places")) {
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

        if (firstValue) {
            writeBit(false);
            writeDouble(value);
            bitsWritten = 1 + DOUBLE_BITS;
            addUnpred(value);
            previousValue = value;
            firstValue = false;
        } else {
            double diff = value - previousValue;
            boolean negative = diff < 0;
            if (negative) diff = -diff;

            long quantIndex = (long) (diff * ebReciprocal) + 1;

            if (quantIndex < UNCOMPRESSIBLE) {
                quantIndex >>= 1;
                int halfIndex = (int) quantIndex;
                quantIndex <<= 1;

                writeBit(true);
                int shiftedIndex = negative ? (RADIUS - halfIndex) : (RADIUS + halfIndex);
                writeInt(shiftedIndex, 16);
                bitsWritten = 1 + 16;

                double recovered = previousValue + (negative ? -1 : 1) * halfIndex * errorBound * 2;
                previousValue = recovered;
            } else {
                writeBit(false);
                writeDouble(value);
                bitsWritten = 1 + DOUBLE_BITS;
                addUnpred(value);
                previousValue = value;
            }
        }

        return bitsWritten;
    }

    private void writeBit(boolean b) {
        int byteIndex = bitPosition / 8;
        int bitIndex = 7 - (bitPosition % 8);
        ensureCapacity(byteIndex + 1);
        if (b) {
            bitBuffer[byteIndex] |= (1 << bitIndex);
        }
        bitPosition++;
    }

    private void writeInt(int value, int numBits) {
        for (int i = numBits - 1; i >= 0; i--) {
            writeBit((value & (1 << i)) != 0);
        }
    }

    private void writeDouble(double value) {
        long bits = Double.doubleToRawLongBits(value);
        for (int i = 63; i >= 0; i--) {
            writeBit((bits & (1L << i)) != 0);
        }
    }

    private void ensureCapacity(int size) {
        if (size > bufferCapacity) {
            int newCapacity = Math.max(size, bufferCapacity * 2);
            byte[] newBuffer = new byte[newCapacity];
            System.arraycopy(bitBuffer, 0, newBuffer, 0, bufferCapacity);
            bitBuffer = newBuffer;
            bufferCapacity = newCapacity;
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

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputPath))) {
            dos.writeDouble(this.errorBound);
            dos.writeInt(RADIUS);
            dos.writeInt(unpredCount);
            dos.writeInt(valueCount);
            metadataBits += DOUBLE_BITS + 96;

            for (int i = 0; i < unpredCount; i++) {
                dos.writeDouble(unpredData[i]);
                metadataBits += DOUBLE_BITS;
            }

            int numBytes = (bitPosition + 7) / 8;
            dos.write(bitBuffer, 0, numBytes);

            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return metadataBits;
    }
}