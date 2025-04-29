package algorithms.DeXOR.encoder;

import algorithms.Encoder;
import algorithms.DeXOR.DeXORTools;
import enums.DataTypeEnums;


public class DoubleDeXOREncoder extends Encoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();
    protected double previous_value = 0;
    protected int previous_q = 0;
    protected int previous_delta = 0;

    protected long previous_exp = 1023;

    protected int rubber_cost = 1;
    protected int contract_step = 0;
    protected int contract_lim = 8;

    protected int buffer_size_bits = 4;
    protected int buffer_size;
    protected double[] buffer;


    public DoubleDeXOREncoder(String outputPath) {
        super(outputPath);
        buffer_size = 1<<buffer_size_bits;
        buffer = new double[buffer_size];
    }


    private void Decimal_XOR(double value) {
//        int q = previous_end;
//
//        boolean flag = DOXRTools.isEnd(value, q); // same end
//        if (!flag) q = DOXRTools.getEnd(value);

        int q = DeXORTools.getEnd(value, previous_q);
        boolean flag = (q == previous_q);


        int delta = 0;
        double alpha = 0;
        while (delta < 16) {
            double pow = DeXORTools.getP10(q + delta);
            double a = DeXORTools.truncate(value / pow) *pow;
            double b = DeXORTools.truncate(previous_value / pow) *pow;
            if (a == b) {
                alpha = a;
                break;
            }
            delta++;
        }

        

        double pow = DeXORTools.getP10(q);
        double beta = value - alpha;
        long beta_star = Math.round((beta) / pow);

        if (delta >= 16 || DeXORTools.comp(alpha+beta_star*pow,value,pow) != 0) { // Exception 10
            out.write(true);
            out.write(true);
            ExceptionHandle(value);
            return;
        }


        beta_star = Math.abs(beta_star);

        if (flag && delta == previous_delta) { //
            // same method 10
            out.write(true);
            out.write(false);
        } else {
            out.write(false); // !flag || dp != pre_dp
            out.write(flag);
            if (!flag) { // 00
                out.write(q + 20, 5);
                previous_q = q;
            }
            out.write(delta, 4);
            previous_delta = delta;
        }

        // extra info
        if (DeXORTools.comp(alpha, 0) == 0) {
            out.write(value > 0); // sign
        }

        out.write(beta_star, DeXORTools.decimalBits(delta));
        this.previous_value = value;

    }

    protected void ExceptionHandle(double value) {
        long lv = Double.doubleToRawLongBits(value);
        long exp = DeXORTools.segment(lv, 2, 12);
        long delta = exp - previous_exp;
        int bias = DeXORTools.getP2(rubber_cost - 1) - 1;
        if (delta >= -bias && delta <= bias) {
            out.write(delta + bias, rubber_cost);
            out.write(lv < 0);
            out.write(lv, 52);

            if (rubber_cost > 1) {
                int su_bias = DeXORTools.getP2(rubber_cost - 2) - 1;
                if (delta >= -su_bias && delta <= su_bias) {
                    contract_step++;
                } else {
                    contract_step = 0;
                }
                if (contract_step == contract_lim) {
                    rubber_cost--;
                    contract_step = 0;
                }
            }
        } else {
            out.write(DeXORTools.getP2(rubber_cost) - 1, rubber_cost);
            out.write(lv, 64);
            contract_step = 0;

            if (rubber_cost < 10) {
                rubber_cost++;
            }
        }
        previous_exp = exp;
    }

    @Override
    public int encode(double value) {
        Decimal_XOR(value);
        return out.track_bits();
    }
}
