package algorithms.DeXOR.decoder;

import algorithms.DeXOR.DeXORTools;

/** Decoder paired with {@code TemporaryAblationEncoder}; it is never used in production. */
public final class TemporaryAblationDecoder extends DoubleDeXORDecoder {
    private final boolean useDecimalXor;
    private final boolean useAdaptiveException;

    public TemporaryAblationDecoder(
            String inputPath, boolean useDecimalXor, boolean useAdaptiveException) {
        super(inputPath, "{rho:8}");
        this.useDecimalXor = useDecimalXor;
        this.useAdaptiveException = useAdaptiveException;
        this.method = new AblationMethod();
    }

    @Override
    protected double ExceptionDecode() {
        return useAdaptiveException ? super.ExceptionDecode() : in.readDouble(64);
    }

    protected final class AblationMethod extends Method {
        @Override
        protected double decodeDouble() {
            if (useDecimalXor) {
                return super.decodeDouble();
            }

            int control = in.readInt(2);
            if (control == 3) {
                return ExceptionDecode();
            }
            if (control == 0 || control == 1) {
                if (control == 0) {
                    previous_q = in.readInt(5) - 20;
                }
                previous_delta = in.readInt(4);
            }

            long sign = in.readBoolean() ? 1 : -1;
            long beta = sign * in.readLong(DeXORTools.decimalBits(previous_delta));
            previous_alpha = 0;
            previous_value = beta * DeXORTools.getP10(previous_q);
            return previous_value;
        }
    }
}
