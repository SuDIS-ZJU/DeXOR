package algorithms.DeXORPlus.decoder;

import algorithms.DeXORPlus.DeXORTools;
import algorithms.DeXORPlus.Predictor;
import algorithms.Decoder;
import enums.DataTypeEnums;

import java.util.HashMap;
import java.util.Map;

public class DoubleDeXORPlusDecoder extends Decoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();
    protected int epsilon = -2;
    protected int p_o_acc = 0;
    protected int p_o = 0;
    protected int p_delta = 0;
    protected int p_q = 0;
    protected double p_alpha = 0;
    private static final int MAX_DELTA_ITERATIONS = 16;
    private static final int DELTA_BITS = 4;
    private static final int Q_BITS_OFFSET = 4;
    protected boolean crossAlphaReuse = true;

    protected Predictor predictor = new Predictor.DELTA();

    public DoubleDeXORPlusDecoder(String inputPath) {
        super(inputPath);
    }

    public DoubleDeXORPlusDecoder(String inputPath, String config) {
        super(inputPath);
        Map<String, String> configMap = parseConfig(config);

        int decimalPlaces = -1;
        if (configMap.containsKey("decimal_places")) {
            try {
                decimalPlaces = Integer.parseInt(configMap.get("decimal_places"));
            } catch (NumberFormatException e) {
            }
        }
        String fallbackDecimalPlaces = configMap.containsKey("fallback_decimal_places")
                ? configMap.get("fallback_decimal_places")
                : configMap.get("failed_decimal_places");
        if (decimalPlaces < 0 && fallbackDecimalPlaces != null) {
            try {
                decimalPlaces = Integer.parseInt(fallbackDecimalPlaces);
            } catch (NumberFormatException e) {
            }
        }
        if (decimalPlaces < 0) decimalPlaces = 2;
        this.epsilon = -decimalPlaces;

        String predictorType = configMap.getOrDefault("predictor", "DELTA").toUpperCase();
        crossAlphaReuse = Boolean.parseBoolean(configMap.getOrDefault("cross_alpha_reuse", "true"));
        int predictorSize = 30;
        if (configMap.containsKey("predictor_size")) {
            try {
                predictorSize = Integer.parseInt(configMap.get("predictor_size"));
            } catch (NumberFormatException e) {
                predictorSize = 30;
            }
        }

        switch (predictorType) {
            case "MAE":
                this.predictor = new Predictor.MAE(predictorSize);
                break;
            case "ARIMA":
                this.predictor = new Predictor.ARIMA(predictorSize);
                break;
            case "DELTA":
            default:
                this.predictor = new Predictor.DELTA();
                break;
        }
    }

    private Map<String, String> parseConfig(String config) {
        Map<String, String> map = new HashMap<>();
        if (config == null || config.isEmpty()) {
            return map;
        }

        if (config.startsWith("{") && config.endsWith("}")) {
            config = config.substring(1, config.length() - 1);
        }

        String[] pairs = config.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    @Override
    public double decodeDouble() {
        double temp = predictor.get();

        double alpha;
        int o, q, delta;

        if (in.readBoolean()) { // case 1 use p_o
            q = epsilon;
            if (in.readBoolean()) {
                delta = p_o - q;
                o = p_o;
            } else {
                delta = in.readInt(DELTA_BITS);
                o = q + delta;
            }
            double pow = DeXORTools.getP10(o);
            if (crossAlphaReuse && predictor instanceof Predictor.DELTA && o == p_o_acc) alpha = p_alpha;
            else alpha = DeXORTools.truncate(temp / pow) * pow;
            p_o = o; // update independently
            p_o_acc = o; // The true previous LCP coordinate
        } else { // case 0 use p_delta + p_q (Equivalent to o but used for distinction)
            if (in.readBoolean()) {
                q = p_q;
            } else {
                q = in.readInt(Q_BITS_OFFSET) + epsilon + 1;
            }

            if (in.readBoolean()) {
                delta = p_delta;
            } else {
                delta = in.readInt(DELTA_BITS);
            }

            o = q + delta;
            double pow = DeXORTools.getP10(o);
            if (crossAlphaReuse && predictor instanceof Predictor.DELTA && q + delta == p_o_acc) alpha = p_alpha;
            else alpha = DeXORTools.truncate(temp / pow) * pow;
            p_q = q; // update independently
            p_delta = delta; // update independently
            p_o_acc = q + delta; // The true previous LCP coordinate
        }
        p_alpha = alpha;

        long sign = alpha > 0 ? 1 : -1;
        if (DeXORTools.comp(alpha, 0) == 0) sign = in.readBoolean() ? 1 : -1; // sign
        long beta = sign * in.readLong(algorithms.DeXOR.DeXORTools.decimalBits(delta));
        double beta_star = beta * algorithms.DeXOR.DeXORTools.getP10(q);

        double value = alpha + beta_star;
        predictor.insert(value);
        return value;
    }
}
