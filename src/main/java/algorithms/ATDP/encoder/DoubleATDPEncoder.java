package algorithms.ATDP.encoder;

import algorithms.Encoder;
import algorithms.ATDP.ATDPTools;
import enums.DataTypeEnums;
import utils.BinaryTools;

public class DoubleATDPEncoder extends Encoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();
    protected double previous_value = 0;
    protected int previous_end = 0;
    protected int previous_dp = 0;

    protected long previous_exp = 1023;

    protected int rubber_cost = 1;
    protected int contract_step = 0;
    protected int contract_lim = 8;

    protected long total = 0;
    protected double total_lead = 0;
    protected double total_tail = 0;


    public DoubleATDPEncoder(String outputPath) {
        super(outputPath);
    }


    private void paint(double value) {
        int e = previous_end;

        boolean flag = ATDPTools.isEnd(value, e); // same end
        if (!flag) e = ATDPTools.getEnd(value);

//        double abs_v = Math.abs(value);
//        int dp = 0;
//        double alpha = 0;
//        while(abs_v >= ATDPTools.getP10(e+dp))dp++;

        int dp = 0;
        double alpha = 0;
        while (dp < 16) {
            double pow = ATDPTools.getP10(e + dp);
            long a = ATDPTools.truncate(value / pow);
            long b = ATDPTools.truncate(previous_value / pow);
            if (a == b) {
                alpha = a * pow;
                break;
            }
            dp++;
        }

        double pow = ATDPTools.getP10(e);
        double beta = value - alpha;
        long beta_star = Math.abs(Math.round((beta) / pow));

//        int lead = BinaryTools.leadZeros((long) (value/pow),64);
//        total_lead += lead;
//        if(lead !=64)total_tail += BinaryTools.tailZeros((long) (value/pow),64);
//        meta.put("CBL",64-(total_lead+total_tail)/total);

        if (dp >= 16) { // overflow Exception 10
            out.write(true);
            out.write(false);
//            out.write(value, 64);
            ExceptionHandle(value);
            return;
        }

        if (flag && dp == previous_dp) { // same method 11
            out.write(true);
            out.write(true);
        } else {
            out.write(false); // !flag || dp != pre_dp
            out.write(flag);
            if (!flag) { // 00
                out.write(e + 20, 5);
                previous_end = e;
            }
            out.write(dp, 4);
            previous_dp = dp;
        }

        // extra info
        if (ATDPTools.comp(alpha, 0) == 0) {
            out.write(value > 0); // sign
        }

        out.write(beta_star, ATDPTools.decimalBits(dp));
        this.previous_value = value;
    }


//    protected void ExceptionHandle(double value) {
//        long lv = Double.doubleToRawLongBits(value);
//        long exp = ATDPTools.segment(lv, 2, 12);
//        if (exp == previous_exp) {
//            out.write(true);
//            out.write(lv < 0);
//            out.write(lv, 52);
//        } else {
//            out.write(false);
//            out.write(lv, 64);
//            previous_exp = exp;
//        }
//    }

    protected void ExceptionHandle(double value) {
        long lv = Double.doubleToRawLongBits(value);
        long exp = ATDPTools.segment(lv, 2, 12);
        long delta = exp - previous_exp;
        int bias = ATDPTools.getP2(rubber_cost - 1) - 1;
        if (delta >= -bias && delta <= bias) {
            out.write(delta + bias, rubber_cost);
            out.write(lv < 0);
            out.write(lv, 52);

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
            out.write(ATDPTools.getP2(rubber_cost) - 1, rubber_cost);
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
//        total ++;
        paint(value);
        return out.track_bits();
    }
}
