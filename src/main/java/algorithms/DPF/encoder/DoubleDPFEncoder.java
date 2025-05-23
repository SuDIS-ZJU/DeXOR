package algorithms.DPF.encoder;

import algorithms.DPF.DPFTools;
import algorithms.DPF.PrefixForest;
import algorithms.Encoder;
import enums.DataTypeEnums;

import java.util.List;


public class DoubleDPFEncoder extends Encoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();

    protected long previous_exp = 1023;

    protected int EL = 1;
    protected int contract_step = 0;
    protected PrefixForest pf = new PrefixForest();
    protected int count = 0;
    /**
     * from config
     **/
    protected int batch_size = 128;
    protected double[] buffer = new double[128];
    protected int[] q = new int[128];
    protected int[] delta = new int[128];
    protected int rho = 8;


    public DoubleDPFEncoder(String outputPath) {
        super(outputPath);
    }

    public DoubleDPFEncoder(String outputPath, String config) {
        super(outputPath, config);
        String batch_size_config = this.config.get("batch_size");
        String rho_config = this.config.get("rho");
        if (batch_size_config != null) {
            batch_size = Integer.parseInt(batch_size_config);
            this.buffer = new double[batch_size];
            this.q = new int[batch_size];
            this.delta = new int[batch_size];
        }
        if (rho_config != null) {
            rho = Integer.parseInt(rho_config);
        }
    }

    protected void ExceptionHandle(double value) {
        long lv = Double.doubleToRawLongBits(value);
        long exp = DPFTools.segment(lv, 2, 12);
        long delta = exp - previous_exp;
        int bias = DPFTools.getP2(EL - 1) - 1;
        if (delta >= -bias && delta <= bias) {
            out.write(delta + bias, EL);
            out.write(lv < 0);
            out.write(lv, 52);

            if (EL > 1) {
                int su_bias = DPFTools.getP2(EL - 2) - 1;
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
            out.write(DPFTools.getP2(EL) - 1, EL);
            out.write(lv, 64);
            contract_step = 0;

            if (EL < 10) {
                EL++;
            }
        }
        previous_exp = exp;
    }

    protected int DPF(int value_num) {
        List<PrefixForest.TreeNode> list = pf.greedy();
        for (PrefixForest.TreeNode node : list) {
            out.write(node.getV().q + 20, 5);
            int delta = node.getDelta();
            out.write(delta, 5);
            out.write(node.getV().prefix, DPFTools.decimalBits(delta));
        }

        for (int i = 0; i < value_num; i++) {
            int j = 0;
            int id = -1;
            int newd = delta[i];
            long beta = 0;
            for (PrefixForest.TreeNode node : list) {
                if (DPFTools.truncate(buffer[i] * DPFTools.getP10(-node.getV().q)) == node.getV().prefix){
                    newd = node.getV().q - q[i];
                    if(newd > 15) continue;
                    id = j;
                    beta = Math.round((buffer[i] - node.getV().value())* DPFTools.getP10(-q[i])) ;
                }
                j++;
            }

            if(newd > 15) {
                out.write(true);
                out.write(true);
                ExceptionHandle(buffer[i]);
            }
            else{
                out.write(false);
                if(id > -1){
                    out.write(true);
                    // huff
                    for(int k=0;k<id-1;k++)out.write(false);
                    out.write(true);
                    //suffix
                    beta = Math.abs(beta);
                    out.write(newd,4);
                    out.write(beta,DPFTools.decimalBits(newd));
                }
            }
        }
        pf.clear();
        return 0;
    }




    @Override
    public int encode(double value) {
        q[count] = DPFTools.getEnd(value, q[(count - 1 + batch_size) % batch_size]);
        buffer[count] = value;
        delta[count] = pf.addChain(buffer[count], q[count]);
        count ++;
        if (value == batch_size) {
            return DPF(batch_size);
        }
        return 0;
//        return out.track_bits();
    }
}
