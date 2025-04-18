package algorithms.DXOR;

public class DOXRTools {
//    private static final double log210 = 1 / Math.log10(2);
    private static final int[] cost = new int[]{0, 4, 7, 10, 14, 17, 20, 24, 27, 30, 34, 37, 40, 44, 47, 50};
    private static final double eps = 1e-23;
    private static final int off = 23;
    private static final double[] P10 = new double[]{1e-23, 1e-22, 1e-21, 1e-20, 1e-19, 1e-18, 1e-17,
            1e-16, 1e-15, 1e-14, 1e-13, 1e-12, 1e-11, 1e-10, 1e-9, 1e-8, 1e-7, 1e-6, 1e-5, 1e-4,
            1e-3, 1e-2, 1e-1, 1, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12,
            1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22, 1e23};

    private static final int[] P2 = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048};

    public static double getP10(int pow) {
        return P10[pow + off];
    }

    public static int getP2(int pow) {
        return P2[pow];
    }

    public static int comp(double a, double b, double eps) {
        double delta = a - b;
        if (delta > eps) return 1;
        if (delta < -eps) return -1;
        return 0;
    }

    public static int comp(double a, double b) {
        double delta = a - b;
        if (delta > eps) return 1;
        if (delta < -eps) return -1;
        return 0;
    }

    public static boolean isInt(double value) {
        return comp(value, Math.round(value), 1e-5) == 0;
    }


    public static boolean isEnd(double value, int end) {
        double alpha = value / getP10(end);
        double beta = value / getP10(end + 1);
        return isInt(alpha) && !isInt(beta);
    }

    public static int getEnd(double value) {
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

    public static int decimalBits(int dp) {
        return cost[dp];
    }

    public static long truncate(double value) {
        if (isInt(value)) return Math.round(value);
        if (value > eps) return (long) Math.floor(value);
        if (value < -eps) return (long) Math.ceil(value);
        return 0;
    }

    public static long segment(long v, int st, int ed) { // 1 - 64;
        int len = ed - st + 1;
        long mask = (1L << len) - 1;
        return (v >> (64 - ed)) & mask;
    }

}
