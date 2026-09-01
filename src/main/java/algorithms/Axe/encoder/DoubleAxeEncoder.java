package algorithms.Axe.encoder;

import algorithms.Axe.AxeDouble;
import algorithms.Encoder;
import enums.DataTypeEnums;

public class DoubleAxeEncoder extends Encoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();

    public DoubleAxeEncoder(String outputPath) {
        super(outputPath);
    }

    public DoubleAxeEncoder(String outputPath, String config) {
        super(outputPath, config);
    }

    @Override
    public int encode(double value) { // sign | base_hash | p | delta | digits
        AxeDouble _v = new AxeDouble(value);
        int digit_size = (int) Math.ceil(Math.log(_v.getBase()) / Math.log(2));
        int delta = _v.getDelta();

        out.write(_v.getSign());
        out.write(_v.getBaseHash(), 2);
        out.write(_v.getP() + 10, 5);
        out.write(delta, 6);

        for (int digit : _v.getDigits()) out.write(digit, digit_size);
        return out.track_bits();
    }
}