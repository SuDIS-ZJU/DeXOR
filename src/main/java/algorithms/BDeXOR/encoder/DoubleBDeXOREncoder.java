package algorithms.BDeXOR.encoder;

import algorithms.BDeXOR.BDeXORTools;
import algorithms.BDeXOR.HuffmanTree;
import algorithms.Encoder;
import enums.DataTypeEnums;

import java.util.Arrays;
import java.util.List;


public class DoubleBDeXOREncoder extends Encoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();

    protected long previous_exp = 1023;

    protected int EL = 1;
    protected int contract_step = 0;

    protected int num = 0;

    protected int[] q_bias_frequency = new int[32]; //  q in [-20,11];
    protected int[] delta_frequency = new int[17]; // delta in [0,15]  16 for Exception

    /**
     * from config
     **/
    protected int batch_size = 1000;
    protected double[] buffer = new double[batch_size];
    protected int[] q = new int[batch_size];
    protected int[] delta = new int[batch_size];
    protected double[] alpha = new double[batch_size];
    protected long[] beta = new long[batch_size];
    protected int rho = 8;


    public DoubleBDeXOREncoder(String outputPath) {
        super(outputPath);
    }

    public DoubleBDeXOREncoder(String outputPath, String config) {
        super(outputPath, config);
        String batch_size_config = this.config.get("batch_size");
        String rho_config = this.config.get("rho");
        if (batch_size_config != null) {
            batch_size = Integer.parseInt(batch_size_config);
            buffer = new double[batch_size];
            q = new int[batch_size];
            delta = new int[batch_size];
            alpha = new double[batch_size];
            beta = new long[batch_size];
        }
        if (rho_config != null) {
            rho = Integer.parseInt(rho_config);
        }
    }

    protected void ExceptionHandle(double value) {
        long lv = Double.doubleToRawLongBits(value);
        long exp = BDeXORTools.segment(lv, 2, 12);
        long delta = exp - previous_exp;
        int bias = BDeXORTools.getP2(EL - 1) - 1;
        if (delta >= -bias && delta <= bias) {
            out.write(delta + bias, EL);
            out.write(lv < 0);
            out.write(lv, 52);

            if (EL > 1) {
                int su_bias = BDeXORTools.getP2(EL - 2) - 1;
                if (delta >= -su_bias && delta <= su_bias) {
                    contract_step++;
                } else {
                    contract_step = 0;
                }
                if (contract_step == rho) {
                    EL--;
                    contract_step = 0;
                }
            }
        } else {
            out.write(BDeXORTools.getP2(EL) - 1, EL);
            out.write(lv, 64);
            contract_step = 0;

            if (EL < 10) {
                EL++;
            }
        }
        previous_exp = exp;
    }

    protected void init() {
        num = 0;
        Arrays.fill(q_bias_frequency, 0);
        Arrays.fill(delta_frequency, 0);
    }


    protected void Decimal_XOR(double value) {
        buffer[num] = value;
        q[num] = BDeXORTools.getEnd(value, q[(num + batch_size - 1) % batch_size]);

        double p_v = buffer[(num + batch_size - 1) % batch_size];
        while (delta[num] < 16) {
            double pow = BDeXORTools.getP10(q[num] + delta[num]);
            long a = BDeXORTools.truncate(value / pow);
            long b = BDeXORTools.truncate(p_v / pow);
            if (a == b) {
                alpha[num] = a * pow;
                break;
            }
            delta[num]++;
        }
        double pow = BDeXORTools.getP10(q[num]);
        double residual = value - alpha[num];
        beta[num] = Math.abs(Math.round((residual) / pow));

        if (delta[num] >= 16 || BDeXORTools.comp(alpha[num] + beta[num] * pow, value, pow) != 0) { // Exception 10
            delta[num] = 16;
        }
        delta_frequency[delta[num]]++;
        q_bias_frequency[q[num] + 20]++;

        num++;
    }

    protected void store() {
        HuffmanTree delta_tree = new HuffmanTree(delta_frequency);
        HuffmanTree q_bias_tree = new HuffmanTree(q_bias_frequency);
        delta_tree.serializeAndStore(this.out);
        q_bias_tree.serializeAndStore(this.out);

        for (int i = 0; i < num; i++) {
            delta_tree.encode(delta[i], this.out);
            if (delta[i] == 16) ExceptionHandle(buffer[i]);
            else {
                q_bias_tree.encode(q[i] + 20, this.out);
                // extra info
                if (BDeXORTools.comp(alpha[i], 0) == 0) {
                    out.write(buffer[i] > 0); // sign
                }
                out.write(beta[i], BDeXORTools.decimalBits(delta[i]));
            }
        }
    }

    @Override
    public int close() {
        if (num > 0) {
            store();
            init();
        }
        return out.track_bits();
    }

    @Override
    public int encode(double value) {
        Decimal_XOR(value);

        if (num == batch_size) {
            store();
            init();
        }
        return out.track_bits();
    }
}
