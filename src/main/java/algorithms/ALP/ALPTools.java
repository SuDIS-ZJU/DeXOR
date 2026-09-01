package algorithms.ALP;

public class ALPTools {
    public static final int MAX_EXPONENT = 18;
    public static final long MAGIC_NUMBER = 0x0018000000000000L;
    public static final long ENCODING_UPPER_LIMIT = 9223372036854774784L;
    public static final long ENCODING_LOWER_LIMIT = -9223372036854774784L;

    private static final long SIGN_BIT_MASK = 0x7FFFFFFFFFFFFFFFL;
    private static final long EXPONENTIAL_BITS_MASK = 0x7FF0000000000000L;
    private static final long NEGATIVE_ZERO_BITS = 0x8000000000000000L;

    private static final double equal_eps = 1e-23;
    private static final double integer_eps = 1e-6;

    private static final double[] FRAC_ARR = {
            1.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0, 10000000.0,
            100000000.0, 1000000000.0, 10000000000.0, 100000000000.0, 1000000000000.0,
            10000000000000.0, 100000000000000.0, 1000000000000000.0, 10000000000000000.0,
            100000000000000000.0, 1000000000000000000.0, 10000000000000000000.0,
            100000000000000000000.0, 1000000000000000000000.0, 10000000000000000000000.0,
            100000000000000000000000.0
    };

    private static final double[] DECI_ARR = {
            1.0, 0.1, 0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.0000001, 0.00000001,
            0.000000001, 0.0000000001, 0.00000000001, 0.000000000001, 0.0000000000001,
            0.00000000000001, 0.000000000000001, 0.0000000000000001, 0.00000000000000001,
            0.000000000000000001, 0.0000000000000000001, 0.00000000000000000001
    };

    private static final long[] FACT_ARR = {
            1, 10, 100, 1000, 10000, 100000, 1000000, 10000000,
            100000000, 1000000000, 10000000000L, 100000000000L, 1000000000000L,
            10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L,
            100000000000000000L, 1000000000000000000L
    };

    private static final int[] P2 = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048};

    private static final int[] cost = {0, 4, 7, 10, 14, 17, 20, 24, 27, 30, 34, 37, 40, 44, 47, 50, 54, 57, 60, 64, 67, 70, 74, 77};

    public static double getFRAC(int idx) {
        return FRAC_ARR[idx];
    }

    public static double getDECI(int idx) {
        return DECI_ARR[idx];
    }

    public static long getFACT(int idx) {
        return FACT_ARR[idx];
    }

    public static int getP2(int pow) {
        return P2[pow];
    }

    public static double getP10(int pow) {
        if (pow >= 0) {
            return FRAC_ARR[pow];
        } else {
            return DECI_ARR[-pow];
        }
    }

    public static long int64ToDoubleToInt64(long x) {
        x = x + MAGIC_NUMBER;
        return Double.doubleToRawLongBits(Double.longBitsToDouble(x)) - MAGIC_NUMBER;
    }

    public static double int64ToDouble(long x) {
        double magic = Double.longBitsToDouble(MAGIC_NUMBER);
        x = x + MAGIC_NUMBER;
        return Double.longBitsToDouble(x) - magic;
    }

    public static boolean isSpecial(double value) {
        long bits = Double.doubleToRawLongBits(value);
        boolean isNaNOrInf = (bits & SIGN_BIT_MASK) >= EXPONENTIAL_BITS_MASK;
        boolean isNegativeZero = bits == NEGATIVE_ZERO_BITS;
        return isNaNOrInf || isNegativeZero;
    }

    public static double getEncodingUpperLimit() {
        return Double.longBitsToDouble(ENCODING_UPPER_LIMIT);
    }

    public static double getEncodingLowerLimit() {
        return Double.longBitsToDouble(ENCODING_LOWER_LIMIT);
    }

    public static int comp(double a, double b, double eps) {
        double delta = a - b;
        if (delta >= eps) return 1;
        if (delta <= -eps) return -1;
        return 0;
    }

    public static int comp(double a, double b) {
        double delta = a - b;
        if (delta > equal_eps) return 1;
        if (delta < -equal_eps) return -1;
        return 0;
    }

    public static boolean isInt(double value) {
        return comp(value, Math.round(value), equal_eps) == 0;
    }

    public static boolean isInt(double value, double eps) {
        return comp(value, Math.round(value), eps) == 0;
    }


    public static boolean isEnd(double value, int end) {
        double alpha = value / getP10(end);
        double beta = value / getP10(end - 1);
        return isInt(alpha) && !isInt(beta);
    }

    public static int getEnd_HP(double value, int last_end) {
        if (isEnd(value, last_end)) return last_end;
        String s = Double.toString(value);
        int index = s.indexOf('.');
        if (index == -1) {
            char[] c = s.toCharArray();
            int e = 0;
            for (int i = c.length - 1; i >= 0; i--) {
                if (c[i] != '0') break;
                e++;
            }
            return e;
        } else {
            return index - (s.length() - 1);
        }
    }

    public static int getEnd(double value, int last_end) {
        if (comp(value, 0, equal_eps) == 0) return 0;
        if (last_end < -12) return getEnd_HP(value, last_end);
        int q = last_end;
        double vq = value / getP10(q);
        if (isInt(vq, integer_eps)) {
            vq = value / getP10(q + 1);
            while (isInt(vq, integer_eps)) {
                q++;
                vq = value / getP10(q + 1);
            }
            return q;
        } else {
            q--;
            vq = value / getP10(q);
            while (!isInt(vq, integer_eps)) {
                q--;
                vq = value / getP10(q);
            }
            return q;
        }
    }

    public static int decimalBits(int dp) {
        return cost[dp];
    }

    public static long truncate(double value) {
        if (isInt(value)) return Math.round(value);
        if (value > equal_eps) return (long) Math.floor(value + integer_eps); // rounding error
        if (value < -equal_eps) return (long) Math.ceil(value - integer_eps);
        return 0;
    }

    public static long segment(long v, int st, int ed) { // 1 - 64;
        int len = ed - st + 1;
        long mask = (1L << len) - 1;
        return (v >> (64 - ed)) & mask;
    }

    public static double epsilon(double value) {
        long lv = Double.doubleToRawLongBits(value);
        long exp = segment(lv, 2, 12);
        return Double.longBitsToDouble((exp - 52) << 52);
    }

    public static class Pair {
        protected int e;
        protected int f;

        public Pair(int e, int f) {
            this.e = e;
            this.f = f;
        }

        public int getE() {
            return e;
        }

        public int getF() {
            return f;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return e == pair.e && f == pair.f;
        }

        @Override
        public int hashCode() {
            return 31 * e + f;
        }
    }
}
