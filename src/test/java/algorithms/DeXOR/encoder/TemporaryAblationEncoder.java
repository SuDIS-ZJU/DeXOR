package algorithms.DeXOR.encoder;

import algorithms.DeXOR.DeXORTools;

/** Test-only DeXOR encoder used to reproduce the module ablation in Section 6.3.2. */
public final class TemporaryAblationEncoder extends DoubleDeXOREncoder {
    private final boolean useDecimalXor;
    private final boolean useAdaptiveException;
    private long exceptionCount;
    private long strictOnlyExceptionCount;
    private long fastRejectedCount;
    private long capacityRejectedCount;

    public TemporaryAblationEncoder(
            String outputPath, boolean useDecimalXor, boolean useAdaptiveException) {
        super(outputPath, "{mode:strict,rho:8}");
        this.useDecimalXor = useDecimalXor;
        this.useAdaptiveException = useAdaptiveException;
        this.method = new AblationMethod();
    }

    @Override
    protected void ExceptionHandle(double value) {
        if (useAdaptiveException) {
            super.ExceptionHandle(value);
        } else {
            // The two-bit exception control code is emitted by Decimal_XOR.
            out.write(value, 64);
        }
    }

    protected final class AblationMethod extends Method {
        @Override
        protected boolean cannotEncode(
                double value, double alpha, long beta, double pow, int delta) {
            boolean capacityRejected = delta >= 16 || beta == Long.MIN_VALUE;
            if (!capacityRejected) {
                int deltaBits = DeXORTools.decimalBits(delta);
                capacityRejected = (Math.abs(beta) >> deltaBits) != 0;
            }
            boolean fastRejected = delta >= 16
                    || DeXORTools.comp(alpha + beta * pow, value, pow) != 0;
            boolean bitwiseRejected = cannotDecodeBitwise(value, alpha, beta, pow, delta);

            if (bitwiseRejected) {
                exceptionCount++;
                if (!fastRejected) strictOnlyExceptionCount++;
            }
            if (fastRejected) fastRejectedCount++;
            if (capacityRejected) capacityRejectedCount++;
            return bitwiseRejected;
        }

        @Override
        protected void Decimal_XOR(double value) {
            if (useDecimalXor) {
                super.Decimal_XOR(value);
                return;
            }

            int q = DeXORTools.getEnd(value, previous_q);
            int delta = 0;
            while (delta < 16) {
                double coordinate = DeXORTools.getP10(q + delta);
                if (DeXORTools.truncate(value / coordinate) == 0) {
                    break;
                }
                delta++;
            }

            double pow = DeXORTools.getP10(q);
            long beta = Math.round(value / pow);
            if (cannotEncode(value, 0, beta, pow, delta)) {
                out.write(true);
                out.write(true);
                ExceptionHandle(value);
                return;
            }

            long magnitude = Math.abs(beta);
            boolean reuseQ = q == previous_q;
            if (reuseQ && delta == previous_delta) {
                out.write(true);
                out.write(false);
            } else {
                out.write(false);
                out.write(reuseQ);
                if (!reuseQ) {
                    out.write(q + 20, 5);
                    previous_q = q;
                }
                out.write(delta, 4);
                previous_delta = delta;
            }

            // With alpha fixed to zero, the suffix sign is always explicit.
            out.write(value > 0);
            out.write(magnitude, DeXORTools.decimalBits(delta));
            previous_value = value;
        }
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    public long getStrictOnlyExceptionCount() {
        return strictOnlyExceptionCount;
    }

    public long getFastRejectedCount() {
        return fastRejectedCount;
    }

    public long getCapacityRejectedCount() {
        return capacityRejectedCount;
    }
}
