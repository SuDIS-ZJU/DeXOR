package algorithms.ATDP.decoder;

import algorithms.Decoder;
import algorithms.ATDP.ATDPTools;
import enums.DataTypeEnums;

public class DoubleATDPDecoder extends Decoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();
    protected double previous_value = 0;
    protected int previous_end = 0;
    protected int previous_dp = 0;

    protected long previous_exp = 0;
    protected int rubber_cost = 1;
    protected int contract_step = 0;
    protected int contract_lim = 8;

    protected double previous_alpha = 0;


    public DoubleATDPDecoder(String inputPath) {
        super(inputPath);
    }

    protected double ExceptionDecode() {
        int bias = ATDPTools.getP2(rubber_cost - 1) - 1;
        long delta = in.readInt(rubber_cost) - bias;
        long lv;
        if (delta >= -bias && delta <= bias) {
            previous_exp += delta;
            lv = in.readLong(1);
            lv = (lv << 11) | previous_exp;
            long seg = in.readLong(52);
            lv = (lv << 52) | seg;

            if (rubber_cost > 1) {
                int su_bias = ATDPTools.getP2(rubber_cost - 2) - 1;
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
            previous_exp = ATDPTools.segment(lv, 2, 12);

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
        if (con == 2) { // overflow Exception
//            return in.readDouble(64);
            return ExceptionDecode();
        }


        if (con == 0 || con == 1) {
            if (con == 0) previous_end = in.readInt(5) - 20;
            previous_dp = in.readInt(4);
            double pow = ATDPTools.getP10(previous_end + previous_dp);
            previous_alpha = ATDPTools.truncate(previous_value / pow) * pow;
        }

        long sign = previous_alpha > 0 ? 1 : -1;
        if (ATDPTools.comp(previous_alpha, 0) == 0) sign = in.readBoolean() ? 1 : -1; // sign
        long beta_star = sign * in.readLong(ATDPTools.decimalBits(previous_dp));
        double beta = beta_star * ATDPTools.getP10(previous_end);

        previous_value = previous_alpha + beta;

        return previous_value;
    }
}
