package algorithms.LZ4.decoder;

import algorithms.Decoder;
import net.jpountz.lz4.LZ4SafeDecompressor;
import net.jpountz.lz4.LZ4Factory;

import java.util.HashMap;
import java.util.Map;

public class DoubleLZ4Decoder extends Decoder {
    private final LZ4SafeDecompressor decompressor;
    private double[] currentBlock;
    private int blockPosition = 0;
    private int blockLength = 0;

    public DoubleLZ4Decoder(String inputPath) {
        this(inputPath, "");
    }
    
    public DoubleLZ4Decoder(String inputPath, String config) {
        super(inputPath);
        this.decompressor = LZ4Factory.fastestInstance().safeDecompressor();
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

            byte[] uncompressed = new byte[valueCount * 8];
            decompressor.decompress(compressed, 0, compressedLength, uncompressed, 0);

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
