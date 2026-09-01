package algorithms.Axe;

import org.example.Main;

import java.util.ArrayList;
import java.util.List;

public class AxeDouble {

    public static final int[] candidate_bases = new int[]{2, 3, 5, 10};
    public static int MAX_CANDIDATE_HASH = 2;

    private double v;
    private int base, base_hash, p, delta;
    private List<Integer> digits;
    private boolean sign;

    private int eps = 52;

    public boolean getSign() {
        return sign;
    }

    public int getBaseHash() {
        return base_hash;
    }

    public int getBase() {
        return base;
    }

    public int getP() {
        return p;
    }

    public int getDelta() {
        return delta;
    }

    public double getV() {
        return v;
    }

    public AxeDouble(double v) {
        this.set(v);
    }

    public AxeDouble(double v, int eps) {
        this.eps = eps;
        this.set(v);
    }

    public void set(double v) {
        this.v = v;
        this.sign = v > 0;

        long ieee_bits;

        if (!sign) v = -v;
        ieee_bits = Double.doubleToRawLongBits(v);
        long exp = ((ieee_bits >> 52) & 0x7FF) - 1023;

        double tol = Math.pow(2, exp - eps - 1);

        int best_digits = Integer.MAX_VALUE;

        double remaining;
        for (int id = 0; id < candidate_bases.length; id++) {
            remaining = v;
            int b = candidate_bases[id];
            double weight = 1.0;
            int now_p = 0;
            while (weight * b <= remaining) {
                weight *= b;
                now_p++;
            }
            while (weight > remaining) {
                weight /= b;
                now_p--;
            }

            List<Integer> now_digits = new ArrayList<>();
            int i = 0;
            while (remaining >= tol && i < eps) {
                int digit = (int) (remaining / weight);
                digit = Math.max(0, Math.min(digit, b - 1));
                now_digits.add(digit);
                remaining -= digit * weight;
                weight /= b;
                i++;
            }
            int now_delta = now_digits.size();
            int now_bits = (int) (now_delta * Math.ceil(Math.log(b) / Math.log(2)));

            if (now_bits < best_digits) {
                best_digits = now_bits;
                this.base_hash = id;
                this.base = b;
                this.p = now_p;
                this.delta = now_delta;
                this.digits = new ArrayList<>(now_digits);
            }
        }
    }

    public int getDigitSize() {
        return (int) Math.ceil(Math.log(base) / Math.log(2));
    }

    public static int getDigitSize(int base) {
        return (int) Math.ceil(Math.log(base) / Math.log(2));
    }

    public static int getBaseMap(int hash_id) {
        return candidate_bases[hash_id];
    }

    public List<Integer> getDigits() {
        return digits;
    }
}
