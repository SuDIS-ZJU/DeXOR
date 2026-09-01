package algorithms.DeXORPlus.encoder;

import algorithms.DeXORPlus.DeXORTools;
import algorithms.DeXORPlus.Predictor;
import algorithms.Encoder;
import enums.DataTypeEnums;

import java.util.HashMap;
import java.util.Map;


public class DoubleDeXORPlusEncoder extends Encoder {
    protected int size = DataTypeEnums.DOUBLE.getSize();
    protected int epsilon = -2;
    protected int p_q = 0;
    protected int p_o = 0;
    protected int p_delta = 0;
    private static final int MAX_DELTA_ITERATIONS = 16;
    private static final int DELTA_BITS = 4;
    private static final int Q_BITS_OFFSET = 4;
    private double total_err = 0;
    private double total_err_2 = 0;
    private long total = 0;


    protected Predictor predictor = new Predictor.DELTA();

    public DoubleDeXORPlusEncoder(String outputPath) {
        super(outputPath);
    }
    
    public DoubleDeXORPlusEncoder(String outputPath, String config) {
        super(outputPath);
        Map<String, String> configMap = parseConfig(config);

        int decimalPlaces =-1;
        if (configMap.containsKey("decimal_places")) {
            try {
                decimalPlaces = Integer.parseInt(configMap.get("decimal_places"));
            } catch (NumberFormatException e) {}
        }
        String fallbackDecimalPlaces = configMap.containsKey("fallback_decimal_places")
                ? configMap.get("fallback_decimal_places")
                : configMap.get("failed_decimal_places");
        if (decimalPlaces < 0 && fallbackDecimalPlaces != null) {
            try {
                decimalPlaces = Integer.parseInt(fallbackDecimalPlaces);
            } catch (NumberFormatException e) {}
        }
        if(decimalPlaces < 0) decimalPlaces =2;
        this.epsilon = -decimalPlaces;

        
        String predictorType = configMap.getOrDefault("predictor", "DELTA").toUpperCase();
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

    protected void Decimal_XOR(double value, double temp) {
        int q = DeXORTools.getEndWithEpsilon(value, epsilon); /// is not real q

        int o = Math.max(epsilon, q);
        int delta = 0;
        double alpha = 0;
        while (delta < MAX_DELTA_ITERATIONS ) {
            double pow = DeXORTools.getP10(o);
            long a = DeXORTools.truncate(value / pow);
            long b = DeXORTools.truncate(temp / pow);
            if (a == b) {
                alpha = a * pow;
                break;
            }
            delta++;
            o++;
        }

        double residual = value - alpha;
        double pow;
        long beta;
        double beta_star;
        if (q <= epsilon) {
            out.write(true);
            pow = DeXORTools.getP10(epsilon);
            beta = DeXORTools.truncate((residual) / pow);
            beta_star = beta * DeXORTools.getP10(epsilon);
            delta = o - epsilon;

            if(o == p_o){ // reuse
                out.write(true);
            }else{
                out.write(false);
                p_o = o;
                out.write(delta, DELTA_BITS);
            }

        } else {
            out.write(false);
            pow = DeXORTools.getP10(q);
            beta = DeXORTools.truncate((residual) / pow);
            beta_star = beta * DeXORTools.getP10(q);
            delta = o - q;

            if(q == p_q){ // reuse
                out.write(true);
            }else{
                out.write(false);
                p_q = q;
                out.write(q - epsilon - 1, Q_BITS_OFFSET);
            }

            if(delta == p_delta){ // reuse
                out.write(true);
            }else{
                out.write(false);
                p_delta = delta;
                out.write(delta, DELTA_BITS);
            }
        }
        beta = Math.abs(beta);
        if (DeXORTools.comp(alpha, 0) == 0) {
            out.write(value > 0); // sign
        }

        out.write(beta, DeXORTools.decimalBits(delta));

        predictor.insert(alpha + beta_star); //使用还原值训练而不是真值
    }

    @Override
    public int encode(double value) {
        total ++;
        double temp = predictor.get();
//        double err = predictor.getError(value);
//        total_err += err;
//        total_err_2 += err * err;
//        meta.put("MAE",total_err / total);
//        meta.put("MSE",total_err_2 / total);
        Decimal_XOR(value,temp);
        return out.track_bits();
    }
}
