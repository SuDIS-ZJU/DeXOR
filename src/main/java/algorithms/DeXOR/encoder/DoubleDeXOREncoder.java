package algorithms.DeXOR.encoder;

import algorithms.DeXOR.DeXORTools;
import algorithms.Encoder;
import enums.DataTypeEnums;


public class DoubleDeXOREncoder extends Encoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();
    protected double previous_value = 0;
    protected int previous_q = 0;
    protected int previous_delta = 0;

    protected long previous_exp = 1023;

    protected int EL = 1;
    protected int contract_step = 0;
    protected boolean skip = false;
    protected Method method = new Native();

    /**
     * from config
     **/
    protected int buffer_bits = 0;
    protected double[] buffer = new double[0];

    protected int rho = 8;

    protected int skip_available = -1;
    protected boolean strictMode = true;
    protected long exceptionCount = 0;
    protected long exceptionPayloadBits = 0;
    protected long exceptionExpandedCount = 0;

    protected boolean cannotDecodeBitwise(double value, double alpha, long beta, double pow, int delta) {
        if (delta >= 16 || beta == Long.MIN_VALUE) {
            return true;
        }

        int deltaBits = DeXORTools.decimalBits(delta);
        long magnitude = Math.abs(beta);
        if ((magnitude >> deltaBits) != 0) {
            return true;
        }

        long sign = alpha > 0 ? 1 : -1;
        if (DeXORTools.comp(alpha, 0) == 0) {
            sign = value > 0 ? 1 : -1;
        }
        double decodedBeta = (sign * magnitude) * pow;
        double decodedValue = alpha + decodedBeta;
        return Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits(decodedValue);
    }

    public DoubleDeXOREncoder(String outputPath) {
        super(outputPath);
    }

    public DoubleDeXOREncoder(String outputPath, String config) {
        super(outputPath, config);
        String buffer_bits_config = this.config.get("buffer_bits");
        String rho_config = this.config.get("rho");
        String skip_available_config = this.config.get("skip_available");
        String mode_config = this.config.get("mode");
        if (buffer_bits_config != null) {
            buffer_bits = Integer.parseInt(buffer_bits_config);
        }
        if (rho_config != null) {
            rho = Integer.parseInt(rho_config);
        }
        if (skip_available_config != null) {
            skip_available = Integer.parseInt(skip_available_config);
        }
        if (mode_config != null) {
            if (mode_config.equalsIgnoreCase("strict")) {
                strictMode = true;
            } else if (mode_config.equalsIgnoreCase("fast")) {
                strictMode = false;
            } else {
                throw new IllegalArgumentException("Unknown DeXOR mode: " + mode_config);
            }
        }

        if (this.buffer_bits > 0) {
            this.buffer = new double[1 << buffer_bits];
            this.method = new Buffered();
        } else if (this.skip_available >= 0) {
            this.method = new Skippable();
        }

    }

    protected void ExceptionHandle(double value) {
        exceptionCount++;
        long lv = Double.doubleToRawLongBits(value);
        long exp = DeXORTools.segment(lv, 2, 12);
        long delta = exp - previous_exp;
        int bias = DeXORTools.getP2(EL - 1) - 1;
        if (delta >= -bias && delta <= bias) {
            exceptionPayloadBits += EL + 53L;
            out.write(delta + bias, EL);
            out.write(lv < 0);
            out.write(lv, 52);

            if (EL > 1) {
                int su_bias = DeXORTools.getP2(EL - 2) - 1;
                if (delta >= -su_bias && delta <= su_bias) {
                    contract_step++;
                } else {
                    contract_step = 0;
                }
                if (contract_step >= Math.max(1, rho)) {
                    EL--;
                    contract_step = 0;
                }
            }
        } else {
            exceptionPayloadBits += EL + 64L;
            exceptionExpandedCount++;
            out.write(DeXORTools.getP2(EL) - 1, EL);
            out.write(lv, 64);
            contract_step = 0;

            if (EL < 10) {
                EL++;
            }
        }
        previous_exp = exp;
        meta.put("metric.exception_count", (double) exceptionCount);
        meta.put("metric.exception_payload_bits", (double) exceptionPayloadBits);
        meta.put("metric.exception_expanded_count", (double) exceptionExpandedCount);
    }

    protected abstract class Method {
        protected boolean cannotEncode(double value, double alpha, long beta, double pow, int delta) {
            if (strictMode) {
                return cannotDecodeBitwise(value, alpha, beta, pow, delta);
            }
            return delta >= 16 || DeXORTools.comp(alpha + beta * pow, value, pow) != 0;
        }

        protected void Decimal_XOR(double value) {
            int q = DeXORTools.getEnd(value, previous_q);

            int delta = 0;
            double alpha = 0;
            while (delta < 16) {
                double pow = DeXORTools.getP10(q + delta);
                long a = DeXORTools.truncate(value / pow);
                long b = DeXORTools.truncate(previous_value / pow);
                if (a == b) {
                    alpha = b * pow;
                    break;
                }
                delta++;
            }
            for (int i = 0; i < delta; i++) {
                String key = Integer.toString(q + i);
                double count = 0;
                if (meta.containsKey(key)) {
                    count = meta.get(key);
                }
                count += 1;
                meta.put(key, count);
            }
            double pow = DeXORTools.getP10(q);
            double residual = value - alpha;
            long beta = Math.round((residual) / pow);

            int delta_bits = DeXORTools.decimalBits(delta);

            if (cannotEncode(value, alpha, beta, pow, delta)) { // Exception 11
                out.write(true);
                out.write(true);
                ExceptionHandle(value);
                return;
            }

            beta = Math.abs(beta);
            boolean flag = q == previous_q;
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

            out.write(beta, delta_bits);
            previous_value = value;
        }

        protected int encode(double value) {
            Decimal_XOR(value);
            return out.track_bits();
        }
    }

    protected class Native extends Method {
    }

    protected class Skippable extends Method {
        protected int exception_times = 0;

        @Override
        protected void Decimal_XOR(double value) {
            if (value != 0.0 && Math.abs(value) < 1e-8) {
                out.write(true);
                out.write(true);
                exception_times++;
                if (exception_times >= skip_available) skip = true;
                ExceptionHandle(value);
                return;
            }
            int q = DeXORTools.getEnd(value, previous_q);

            int delta = 0;
            double alpha = 0;
            while (delta < 16) {
                double pow = DeXORTools.getP10(q + delta);
                long a = DeXORTools.truncate(value / pow);
                long b = DeXORTools.truncate(previous_value / pow);
                if (a == b) {
                    alpha = a * pow;
                    break;
                }
                delta++;
            }
            double pow = DeXORTools.getP10(q);
            double residual = value - alpha;
            long beta = Math.round((residual) / pow);

            if (cannotEncode(value, alpha, beta, pow, delta)) { // Exception 10
                out.write(true);
                out.write(true);
                exception_times++;
                if (exception_times >= skip_available) skip = true;
                ExceptionHandle(value);
                return;
            }

            exception_times = 0;
            beta = Math.abs(beta);
            boolean flag = q == previous_q;
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

            out.write(beta, DeXORTools.decimalBits(delta));
            previous_value = value;
        }

        protected int encode(double value) {
            if (skip) ExceptionHandle(value);
            else Decimal_XOR(value);
            return out.track_bits();
        }
    }

    protected class Buffered extends Method {
        protected int total = 0;

        @Override
        protected void Decimal_XOR(double value) {
            if (value != 0.0 && Math.abs(value) < 1e-8) {
                out.write(true);
                out.write(true);
                ExceptionHandle(value);
                return;
            }
            int q = DeXORTools.getEnd(value, previous_q);

            int delta = 0;
            double alpha = 0;
            int id = 0;

            while (delta < 16) {
                double pow = DeXORTools.getP10(q + delta);
                long a = DeXORTools.truncate(value / pow);
                long b = DeXORTools.truncate(buffer[0] / pow);
                if (a == b) {
                    alpha = a * pow;
                    break;
                }
                delta++;
            }

            double pow = DeXORTools.getP10(q + delta - 1);
            long a = DeXORTools.truncate(value / pow);

            for (int i = 1; delta > 0 && i < buffer.length; i++) {
                long b = DeXORTools.truncate(buffer[i] / pow);
                while (delta > 0 && a == b) {
                    alpha = a * pow;
                    id = i;
                    delta--;
                    pow = DeXORTools.getP10(q + delta - 1);
                    a = DeXORTools.truncate(value / pow);
                    b = DeXORTools.truncate(buffer[i] / pow);
                }
            }


            double q_pow = DeXORTools.getP10(q);
            double residual = value - alpha;
            long beta = Math.round((residual) / q_pow);

            if (cannotEncode(value, alpha, beta, q_pow, delta)) { // Exception 10
                out.write(true);
                out.write(true);
                ExceptionHandle(value);
                return;
            }

            beta = Math.abs(beta);
            boolean flag = q == previous_q;
            if (flag && delta == previous_delta) { //
                // same method 10
                out.write(true);
                out.write(false);
                out.write(id, buffer_bits);
            } else {
                out.write(false); // !flag || dp != pre_dp
                out.write(flag);
                out.write(id, buffer_bits);
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

            out.write(beta, DeXORTools.decimalBits(delta));
            buffer[total++] = value;

            total %= buffer.length;
        }
    }


    @Override
    public int encode(double value) {
        return this.method.encode(value);
    }
}
