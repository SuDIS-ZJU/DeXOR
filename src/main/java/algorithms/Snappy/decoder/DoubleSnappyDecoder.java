package algorithms.Snappy.decoder;

import algorithms.Decoder;
import org.xerial.snappy.Snappy;

public class DoubleSnappyDecoder extends Decoder {
    private double[] currentBlock;
    private int blockPosition = 0;
    private int blockLength = 0;

    public DoubleSnappyDecoder(String inputPath) {
        this(inputPath, "");
    }
    
    public DoubleSnappyDecoder(String inputPath, String config) {
        super(inputPath);
    }

    @Override
    public double decodeDouble() {
        if (blockPosition >= blockLength) {
            if (!readNextBlock()) {
                return 0;
            }
        }
        return currentBlock[blockPosition++];
    }

    private boolean readNextBlock() {
        try {
            int valueCount = (int) in.readLong(32);
            int compressedLength = (int) in.readLong(32);

            if (valueCount <= 0 || compressedLength <= 0) {
                return false;
            }

            byte[] compressed = new byte[compressedLength];
            for (int i = 0; i < compressedLength; i++) {
                compressed[i] = (byte) in.readLong(8);
            }

            int uncompressedLength = Snappy.uncompressedLength(compressed, 0, compressedLength);
            byte[] uncompressed = new byte[uncompressedLength];
            Snappy.uncompress(compressed, 0, compressedLength, uncompressed, 0);

            currentBlock = new double[valueCount];
            for (int i = 0; i < valueCount; i++) {
                long bits = 0;
                for (int j = 0; j < 8; j++) {
                    bits = (bits << 8) | (uncompressed[i * 8 + j] & 0xFF);
                }
                currentBlock[i] = Double.longBitsToDouble(bits);
            }

            blockPosition = 0;
            blockLength = valueCount;

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}