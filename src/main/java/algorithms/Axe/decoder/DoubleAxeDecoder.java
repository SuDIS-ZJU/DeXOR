package algorithms.Axe.decoder;

import algorithms.Axe.AxeDouble;
import algorithms.Decoder;
import enums.DataTypeEnums;

public class DoubleAxeDecoder extends Decoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();

    public DoubleAxeDecoder(String inputPath) {
        super(inputPath);
    }

    public DoubleAxeDecoder(String inputPath, String config) {
        super(inputPath, config);
    }

    @Override
    public double decodeDouble() {
        boolean sign = in.readBoolean();
        int base_hash = in.readInt(2);
        int base = AxeDouble.getBaseMap(base_hash);
        int p = in.readInt(5) - 10;
        int delta = in.readInt(6);
        int digit_size = AxeDouble.getDigitSize(base);

        int _p = 0;
        double weight = 1.0;
        double base_inv = 1.0 / base;
        while (_p < p) {
            _p++;
            weight *= base;
        }
        while (_p > p) {
            _p--;
            weight *= base_inv;
        }

        double res = 0;
        for (int i = 0; i < delta; i++) {
            int digit = in.readInt(digit_size);
            res += digit * weight;
            weight *= base_inv;
        }
        if (!sign) res = -res;
        return res;
    }
}