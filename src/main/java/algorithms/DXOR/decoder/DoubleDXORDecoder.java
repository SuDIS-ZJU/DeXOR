package algorithms.DXOR.decoder;

import algorithms.Decoder;
import algorithms.DXOR.DOXRTools;
import enums.DataTypeEnums;

public class DoubleDXORDecoder extends Decoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();
    protected double previous_value = 0;
    protected int previous_q = 0;
    protected int previous_delta = 0;

    protected long previous_exp = 0;
    protected int rubber_cost = 1;
    protected int contract_step = 0;
    protected int contract_lim = 8;

    protected double previous_alpha = 0;


    public DoubleDXORDecoder(String inputPath) {
        super(inputPath);
    }

    protected double ExceptionDecode() {
        int bias = DOXRTools.getP2(rubber_cost - 1) - 1;
        long delta = in.readInt(rubber_cost) - bias;
        long lv;
        if (delta >= -bias && delta <= bias) {
            previous_exp += delta;
            lv = in.readLong(1);
            lv = (lv << 11) | previous_exp;
            long seg = in.readLong(52);
            lv = (lv << 52) | seg;

            if (rubber_cost > 1) {
                int su_bias = DOXRTools.getP2(rubber_cost - 2) - 1;
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
            lv = in.readLong(64);
            previous_exp = DOXRTools.segment(lv, 2, 12);

            if (rubber_cost < 10) {
                rubber_cost++;
                contract_step = 0;
            }
        }
        return Double.longBitsToDouble(lv);
    }

    @Override
    public double decodeDouble() {
        int con = in.readInt(2);
        if (con == 3) { // overflow Exception
//            return in.readDouble(64);
            return ExceptionDecode();
        }


        if (con == 0 || con == 1) {
            if (con == 0) previous_q = in.readInt(5) - 20;
            previous_delta = in.readInt(4);
            double pow = DOXRTools.getP10(previous_q + previous_delta);
            previous_alpha = DOXRTools.truncate(previous_value / pow) * pow;
        }

        long sign = previous_alpha > 0 ? 1 : -1;
        if (DOXRTools.comp(previous_alpha, 0) == 0) sign = in.readBoolean() ? 1 : -1; // sign
        long beta_star = sign * in.readLong(DOXRTools.decimalBits(previous_delta));
        double beta = beta_star * DOXRTools.getP10(previous_q);

        previous_value = previous_alpha + beta;

        return previous_value;
    }
}
