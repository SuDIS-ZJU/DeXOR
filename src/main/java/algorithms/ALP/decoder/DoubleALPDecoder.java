package algorithms.ALP.decoder;

import algorithms.ALP.ALPTools;
import algorithms.Decoder;

public class DoubleALPDecoder extends Decoder {
    private static final int VECTOR_SIZE = 1024;
    private static final int N_VECTORS = 100;  // Match C++ N_VECTORS_PER_ROWGROUP

    protected double[] buffer = new double[VECTOR_SIZE];
    protected int available = 0;

    public DoubleALPDecoder(String inputPath) {
        super(inputPath);
    }

    public DoubleALPDecoder(String inputPath, String config) {
        super(inputPath, config);
    }

    protected long[] decompressFFOR() {
        long[] res = new long[VECTOR_SIZE];

        int bits = in.readInt(8);
        long minv = in.readLong(64);

        for (int i = 0; i < VECTOR_SIZE; i++) {
            long v = in.readLong(bits);
            res[i] = v + minv;
        }
        return res;
    }

    protected void decompress() {
        int e = in.readInt(5);
        int f = in.readInt(5);

        int excCount = in.readInt(16);
        int[] excIds = new int[excCount];
        double[] excVals = new double[excCount];

        for (int i = 0; i < excCount; i++) {
            excIds[i] = in.readInt(16);
            excVals[i] = in.readDouble(64);
        }

        long[] encVec = decompressFFOR();

        for (int i = 0; i < VECTOR_SIZE; i++) {
            buffer[i] = encVec[i] * ALPTools.getFRAC(f) * ALPTools.getDECI(e);
        }

        for (int i = 0; i < excCount; i++) {
            buffer[excIds[i]] = excVals[i];
        }

        available = VECTOR_SIZE;
    }

    @Override
    public double decodeDouble() {
        if (available == 0) {
            decompress();
        }
        double result = buffer[VECTOR_SIZE - available];
        available--;
        return result;
    }
}