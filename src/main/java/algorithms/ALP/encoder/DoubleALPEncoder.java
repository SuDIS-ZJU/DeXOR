package algorithms.ALP.encoder;

import algorithms.ALP.ALPTools;
import algorithms.Encoder;
import enums.DataTypeEnums;
import utils.BinaryTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoubleALPEncoder extends Encoder {
    private static final int VECTOR_SIZE = 1024;
    private static final int N_VECTORS = 100;
    private static final int SAMPLES_PER_VECTOR = 32;
    private static final int ROWGROUP_VECTOR_SAMPLES = 8;
    private static final int K_COMBINATIONS = 5;

    protected int size = DataTypeEnums.DOUBLE.getSize();
    protected double[][] group = new double[N_VECTORS][VECTOR_SIZE];
    protected int r = 0;
    protected int c = 0;

    protected int n = N_VECTORS;
    protected int m = SAMPLES_PER_VECTOR;
    protected int k = K_COMBINATIONS;
    protected int w = 32;

    public DoubleALPEncoder(String outputPath) {
        super(outputPath);
    }

    public DoubleALPEncoder(String outputPath, String config) {
        super(outputPath, config);
    }

    protected long encodeValue(double value, int fac, int exp) {
        double scaled = value * ALPTools.getFRAC(exp) * ALPTools.getDECI(fac);
        long encoded = Math.round(scaled);
        double decoded = encoded * ALPTools.getFACT(fac) * ALPTools.getDECI(exp);
        if (Double.doubleToRawLongBits(decoded) != Double.doubleToRawLongBits(value)) {
            return ALPTools.ENCODING_UPPER_LIMIT;
        }
        return ALPTools.int64ToDoubleToInt64(encoded);
    }

    protected ALPTools.Pair[] firstSampling(int filledVectors) {
        int samplesPerVector = m;

        Map<ALPTools.Pair, Integer> pairCount = new HashMap<>();

        for (int vecIdx = 0; vecIdx < filledVectors; vecIdx++) {
            for (int i = 0; i < samplesPerVector; i++) {
                int idx = (i * VECTOR_SIZE) / samplesPerVector;
                double v = group[vecIdx][idx];

                for (int e = ALPTools.MAX_EXPONENT; e >= 0; e--) {
                    for (int f = e; f >= 0; f--) {
                        long enc = encodeValue(v, f, e);
                        if (enc != ALPTools.ENCODING_UPPER_LIMIT) {
                            ALPTools.Pair p = new ALPTools.Pair(e, f);
                            pairCount.put(p, pairCount.getOrDefault(p, 0) + 1);
                        }
                    }
                }
            }
        }

        List<Map.Entry<ALPTools.Pair, Integer>> sorted = new ArrayList<>(pairCount.entrySet());
        sorted.sort((a, b) -> {
            int countA = a.getValue();
            int countB = b.getValue();
            if (countB != countA) {
                return countB - countA;
            }
            int expA = a.getKey().getE();
            int expB = b.getKey().getE();
            if (expA != expB) {
                return expB - expA;
            }
            int facA = a.getKey().getF();
            int facB = b.getKey().getF();
            return facB - facA;
        });

        ALPTools.Pair[] combinations = new ALPTools.Pair[k];
        for (int i = 0; i < k && i < sorted.size(); i++) {
            combinations[i] = sorted.get(i).getKey();
        }

        return combinations;
    }

    protected ALPTools.Pair secondSampling(double[] row, ALPTools.Pair[] combinations) {
        int samplesPerCombination = w;

        ALPTools.Pair best = new ALPTools.Pair(0, 0);
        long bestEstimatedSize = Long.MAX_VALUE;
        int consecutiveWorse = 0;

        for (int i = 0; i < k && combinations[i] != null; i++) {
            ALPTools.Pair p = combinations[i];
            int e = p.getE();
            int f = p.getF();
            int exceptions = 0;
            long minEncoded = Long.MAX_VALUE;
            long maxEncoded = Long.MIN_VALUE;

            for (int j = 0; j < samplesPerCombination; j++) {
                int idx = (j * VECTOR_SIZE) / samplesPerCombination;
                double v = row[idx];

                long enc = encodeValue(v, f, e);
                if (enc != ALPTools.ENCODING_UPPER_LIMIT) {
                    if (enc < minEncoded) minEncoded = enc;
                    if (enc > maxEncoded) maxEncoded = enc;
                } else {
                    exceptions++;
                }
            }

            long estimatedSize = 0;
            if (exceptions < samplesPerCombination) {
                int forBitWidth = countBits(maxEncoded - minEncoded);
                estimatedSize = (long) samplesPerCombination * forBitWidth + 
                              (long) exceptions * (64 + 16);
            } else {
                estimatedSize = Long.MAX_VALUE;
            }

            if (i == 0 || estimatedSize < bestEstimatedSize) {
                best = p;
                bestEstimatedSize = estimatedSize;
                consecutiveWorse = 0;
            } else {
                consecutiveWorse++;
                if (consecutiveWorse >= 2) {
                    break;
                }
            }
        }

        return best;
    }

    private int countBits(long value) {
        if (value == 0) return 0;
        return 64 - Long.numberOfLeadingZeros(value);
    }

    protected void FFOR(long[] encVec) {
        long minv = Long.MAX_VALUE;
        for (int i = 0; i < VECTOR_SIZE; i++) {
            minv = Math.min(encVec[i], minv);
        }

        long maxv = Long.MIN_VALUE;
        for (int i = 0; i < VECTOR_SIZE; i++) {
            long delta = encVec[i] - minv;
            encVec[i] = delta;
            maxv = Math.max(delta, maxv);
        }

        int bits = maxv == 0 ? 1 : 64 - Long.numberOfLeadingZeros(maxv);

        out.write(bits, 8);
        out.write(minv, 64);

        for (int i = 0; i < VECTOR_SIZE; i++) {
            out.write(encVec[i], bits);
        }
    }

    protected void reset() {
        for (int i = 0; i < N_VECTORS; i++) {
            for (int j = 0; j < VECTOR_SIZE; j++) {
                group[i][j] = 0;
            }
        }
    }

    protected int getFilledVectors() {
        if (r == 0 && c == 0) return 0;
        if (c == 0) return r;
        return r + 1;
    }

    protected void ALP() {
        int filledVectors = getFilledVectors();
        ALPTools.Pair[] combinations = firstSampling(filledVectors);
        double upperLimit = ALPTools.getEncodingUpperLimit();

        for (int i = 0; i < filledVectors; i++) {
            ALPTools.Pair best = secondSampling(group[i], combinations);
            int e = best.getE();
            int f = best.getF();

            out.write(e, 5);
            out.write(f, 5);

            long[] encVec = new long[VECTOR_SIZE];
            double[] workVec = new double[VECTOR_SIZE];

            for (int j = 0; j < VECTOR_SIZE; j++) {
                double v = group[i][j];
                if (ALPTools.isSpecial(v)) {
                    workVec[j] = upperLimit;
                } else {
                    workVec[j] = v;
                }
            }

            int[] excIds = new int[VECTOR_SIZE];
            double[] excVals = new double[VECTOR_SIZE];
            int excCount = 0;

            long firstSuccess = 0;
            boolean haveFirst = false;

            for (int j = 0; j < VECTOR_SIZE; j++) {
                double v = workVec[j];
                long enc = encodeValue(v, f, e);

                if (enc != ALPTools.ENCODING_UPPER_LIMIT) {
                    if (!haveFirst) {
                        haveFirst = true;
                        firstSuccess = enc;
                    }
                    encVec[j] = enc;
                } else {
                    excIds[excCount] = j;
                    excVals[excCount] = group[i][j];
                    excCount++;
                }
            }

            out.write(excCount, 16);

            for (int j = 0; j < excCount; j++) {
                out.write(excIds[j], 16);
                out.write(excVals[j], 64);
                encVec[excIds[j]] = firstSuccess;
            }

            FFOR(encVec);
        }

        reset();
    }

    @Override
    public int close() {
        if (c > 0 || r > 0) {
            ALP();
        }
        out.clear();
        return out.track_bits();
    }

    @Override
    public int encode(double value) {
        group[r][c] = value;
        c++;
        if (c == VECTOR_SIZE) {
            if (r >= n - 1) {
                ALP();
                r = 0;
            } else {
                r++;
            }
            c = 0;
        }
        return out.track_bits();
    }
}