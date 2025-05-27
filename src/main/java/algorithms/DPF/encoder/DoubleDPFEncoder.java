package algorithms.DPF.encoder;

import algorithms.DPF.DPFTools;
import algorithms.DPF.PrefixForest;
import algorithms.DPF.TreeNode;
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
    protected int batch_bits = 7;
    protected double[] buffer = new double[128];
    protected int[] q = new int[128];
    protected int[] delta = new int[128];
    protected int rho = 8;


    public DoubleDPFEncoder(String outputPath) {
        super(outputPath);
    }

    public DoubleDPFEncoder(String outputPath, String config) {
        super(outputPath, config);
        String batch_bits_config = this.config.get("batch_bits");
        String rho_config = this.config.get("rho");
        if (batch_bits_config != null) {
            this.batch_bits = Integer.parseInt(batch_bits_config);
            this.batch_size = 1 << batch_bits;
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

    protected int storeList(List<TreeNode> list) {
        out.write(list.size(), batch_bits + 4); // numbers of nodes < batch_size * 16

        for (TreeNode node : list) {
            out.write(node.getQ() + 20, 5);
            out.write(node.getPrefix(), node.getCost());
            out.write(node.getDelta(), 4);
        }

        return DPFTools.getBitLength(list.size() + 2); // 2 case for mark
    }

    protected int DPF() {
        List<TreeNode> list = pf.getList(batch_size);
        int b_length = storeList(list);

        for (int i = 0; i < count; i++) {
            int j = 0;

            int id = -1;
            int newd = delta[i];

            for (TreeNode node : list) {
                if (DPFTools.truncate(buffer[i] * DPFTools.getP10(-node.getQ())) == node.getDelta()) {
                    newd = node.getQ() - q[i];
                    if (newd > 15) continue;
                    id = j;
                }
                j++;
            }

            int o = q[i] + newd;
            double alpha = DPFTools.truncate(buffer[i] * DPFTools.getP10(-o)) * DPFTools.getP10(o);
            long beta = Math.round((buffer[i] - alpha) * DPFTools.getP10(-q[i]));

            if (newd > 15) {
                out.write(0, b_length);
                ExceptionHandle(buffer[i]);
            } else {
                out.write(id + 2, b_length);
                if (id == -1) {
                    out.write(false);
                    out.write(alpha > 0);
                }

                //suffix
                beta = Math.abs(beta);
                out.write(newd, 4);
                out.write(beta, DPFTools.decimalBits(newd));
            }
        }

        count = 0;
        pf.init();
        return 0;
    }


    @Override
    public int encode(double value) {
        q[count] = DPFTools.getEnd(value, q[(count - 1 + batch_size) % batch_size]);
        delta[count] = pf.addChain(value, q[count]);
        buffer[count] = value;
        count++;
        if (count == batch_size) {
            return DPF();
        }
        return out.track_bits();
    }
}
