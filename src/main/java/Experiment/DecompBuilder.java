package Experiment;

import algorithms.AlgorithmsManager;
import algorithms.Decoder;
import enums.DataTypeEnums;
import utils.TableStreamer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecompBuilder {
    private static final double[] EPS = new double[]{1, 1e-1, 1e-2, 1e-3, 1e-4, 1e-5, Math.nextUp(1e-6), 1e-7, 1e-8, 1e-9, 1e-10, 1e-11, 1e-12,
            1e-13, 1e-14, 1e-15, Math.nextUp(1e-16), 1e-17, 1e-18, 1e-19, Math.nextUp(1e-20), 1e-21, 1e-22, Math.nextUp(1e-23)}; // rounds up; ensure eps >= intended value


    private final Decoder decoder;
    private TableStreamer table;

    private final DataTypeEnums dataType;
    private final String algorithm_name;

    private final String input_path;
    private final String config_path;

    private String table_name;

    private int targetCol = 1;
    private String targetColName = "";
    private VerificationMode verificationMode = VerificationMode.BITWISE;
    private int decimalPlaces = -1;
    private boolean standalone = false;

    private long total = 0;
    private long error_id = 0;
    private long rawErrorCount = 0;
    private long precisionErrorCount = 0;
    private long boundedErrorCount = 0;
    private double rawRelativeErrorSum = 0;
    private double rawRelativeErrorMax = 0;
    private long rawRelativeErrorSamples = 0;
    private double precisionRelativeErrorSum = 0;
    private double precisionRelativeErrorMax = 0;
    private long precisionRelativeErrorSamples = 0;

    private double finish_time = 0;
    private Map<String, String> info = new HashMap<>();

    public Map<String, String> getInfo() {
        return info;
    }

    public DecompBuilder(DataTypeEnums dataType, String algorithm_name, String table_name, String table_path, String input_path, String config_path) throws Exception {
        this(dataType, algorithm_name, table_name, table_path, input_path, config_path, 1, "");
    }

    public DecompBuilder(DataTypeEnums dataType, String algorithm_name, String table_name, String table_path, String input_path, String config_path, int col, String colName) throws Exception {
        this(dataType, algorithm_name, table_name, table_path, input_path, config_path, col, colName, -1);
    }

    public DecompBuilder(DataTypeEnums dataType, String algorithm_name, String table_name, String table_path, String input_path, String config_path, int col, String colName, int decimalPlaces) throws Exception {
        this.dataType = dataType;
        this.algorithm_name = algorithm_name;
        this.table_name = table_name;
        this.input_path = input_path;
        this.config_path = config_path;
        this.targetCol = col;
        this.targetColName = colName;
        this.decimalPlaces = decimalPlaces;

        String config = null;
        if (config_path != null && !config_path.isEmpty()) {
            config = seekConfig();
        }

        config = withDecimalPlaces(config, decimalPlaces);

        verificationMode = verificationModeFromConfig(config);

        this.decoder = AlgorithmsManager.getDecoder(dataType.getType(), algorithm_name, input_path, config);

        if (table_path != null && !table_path.isEmpty()) {
            this.table = new TableStreamer(table_path);
        }
    }

    private String withDecimalPlaces(String config, int places) {
        if (config == null || config.trim().isEmpty()) {
            return "decimal_places:" + places;
        }
        Matcher matcher = Pattern.compile("(^|,)\\s*decimal_places\\s*:[^,]*").matcher(config);
        if (matcher.find()) {
            return matcher.replaceFirst("$1decimal_places:" + places);
        }
        return config + ",decimal_places:" + places;
    }

    public DecompBuilder(DataTypeEnums dataType, String algorithm_name, String input_path) throws Exception {
        this(dataType, algorithm_name, input_path, null);
    }

    public DecompBuilder(DataTypeEnums dataType, String algorithm_name, String input_path, String config_path) throws Exception {
        this.dataType = dataType;
        this.algorithm_name = algorithm_name;
        this.input_path = input_path;
        this.config_path = config_path;
        this.standalone = true;
        this.table_name = "";
        String config = null;
        if (config_path != null && !config_path.isEmpty()) config = seekConfig();
        if (config == null || config.isEmpty()) {
            config = "";
        }
        verificationMode = verificationModeFromConfig(config);
        if (config == null || config.isEmpty())
            this.decoder = AlgorithmsManager.getDecoder(dataType.getType(), algorithm_name, input_path);
        else this.decoder = AlgorithmsManager.getDecoder(dataType.getType(), algorithm_name, input_path, config);
    }

    public void setVerify(boolean verify) {
        this.verificationMode = verify ? VerificationMode.BITWISE : VerificationMode.NONE;
    }

    public void setVerificationMode(VerificationMode verificationMode) {
        this.verificationMode = verificationMode == null ? VerificationMode.BITWISE : verificationMode;
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public String seekConfig() {
        Pattern algoPattern = Pattern.compile(algorithm_name + "\\{([^}]*)\\}");
        Pattern globalPattern = Pattern.compile("global\\{([^}]*)\\}");
        String line;
        String algoConfig = null;
        String globalConfig = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(config_path))) {
            while ((line = reader.readLine()) != null) {
                Matcher algoMatcher = algoPattern.matcher(line);
                if (algoMatcher.find()) {
                    algoConfig = algoMatcher.group(1);
                }
                Matcher globalMatcher = globalPattern.matcher(line);
                if (globalMatcher.find()) {
                    globalConfig = globalMatcher.group(1);
                }
            }
        } catch (IOException e) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        if (globalConfig != null) {
            result.append(globalConfig);
        }
        if (algoConfig != null) {
            if (result.length() > 0) result.append(",");
            result.append(algoConfig);
        }

        return result.length() > 0 ? result.toString() : null;
    }

    private VerificationMode verificationModeFromConfig(String config) {
        if (config == null || config.isEmpty()) return VerificationMode.BITWISE;
        for (String pair : config.split(",")) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2 && keyValue[0].trim().equalsIgnoreCase("verification_mode")) {
                return VerificationMode.parse(keyValue[1]);
            }
        }
        return VerificationMode.BITWISE;
    }


    //todo add other types
    public void test_decompress() {
        if (dataType.equals(DataTypeEnums.DOUBLE)) {
            test_decompressDouble();
        }
    }

    protected String result_format(double v) {
        return String.format("%.2f", v);

    }

    public static int getDecimalPlace(double value) {
        String s = Double.toString(value);
        int index = s.indexOf('.');
        if (index == -1) {
            return 0;
        } else {
            return (s.length() - 1) - index;
        }
    }

    private void test_decompressDouble() {
        if (standalone) {
            standaloneDecompress();
            return;
        }

        if (table != null) {
            try {
                table.reset();
            } catch (Exception e) {
            }

            while (true) {
                double original;
                try {
                    original = table.getDouble(targetCol);
                } catch (Exception invalidValue) {
                    try {
                        table.next();
                        continue;
                    } catch (Exception eof) {
                        break;
                    }
                }

                try {
                    long start_time = System.nanoTime();
                    double decoded = decoder.decodeDouble();
                    long end_time = System.nanoTime();
                    finish_time += (double) (end_time - start_time) / 1000000;
                    total++;

                    if (verificationMode != VerificationMode.NONE) {
                        verifyDecodedValue(original, decoded);
                    }

                    try {
                        table.next();
                    } catch (Exception eof) {
                        break;
                    }
                } catch (Exception decodeFailure) {
                    break;
                }
            }
        }

        double decomp_speed = finish_time == 0 ? 0 : (double) (total * dataType.getSize() / 8) / finish_time;
        long selectedErrors;
        if (decimalPlaces >= 0) {
            selectedErrors = boundedErrorCount;
        } else {
            selectedErrors = verificationMode == VerificationMode.PRECISION
                    ? precisionErrorCount : rawErrorCount;
        }
        boolean failed = verificationMode != VerificationMode.NONE && selectedErrors > 0;
        String displayName = targetColName.isEmpty() ? table_name : table_name + "." + targetColName;
        String validationName = decimalPlaces >= 0
                ? "absolute_error_1e-" + decimalPlaces
                : verificationMode.getConfigName();

        if (failed) {
            System.out.println(algorithm_name + " decompress \"" + displayName + "\" ["
                    + validationName + "] completed with " + selectedErrors + " errors.");
            info.put("error", "First error at " + error_id + "; total errors=" + selectedErrors);
        } else {
            System.out.println(algorithm_name + " decompress \"" + displayName + "\" ["
                    + validationName + "] success! Total " + total
                    + " values. Finish time is "
                    + result_format(finish_time) + "ms");
        }
        putMetrics(decomp_speed);
    }

    private void verifyDecodedValue(double original, double decoded) {
        if (decimalPlaces >= 0) {
            int place = Math.min(decimalPlaces, EPS.length - 1);
            if (Math.abs(original - decoded) > EPS[place]) {
                boundedErrorCount++;
                if (error_id == 0) error_id = total;
            }
            return;
        }

        boolean rawMismatch = Double.doubleToRawLongBits(original) != Double.doubleToRawLongBits(decoded);
        if (!rawMismatch) return;

        rawErrorCount++;
        recordRawRelativeError(original, decoded);

        int q = decoder.getPrecisionPosition();
        boolean precisionMismatch = q == Integer.MIN_VALUE || !sameAtPrecision(original, decoded, q);
        if (precisionMismatch) {
            precisionErrorCount++;
            recordPrecisionRelativeError(original, decoded, q);
        }

        boolean selectedMismatch = verificationMode == VerificationMode.BITWISE || precisionMismatch;
        if (selectedMismatch && error_id == 0) error_id = total;
    }

    private boolean sameAtPrecision(double original, double decoded, int q) {
        if (!Double.isFinite(original) || !Double.isFinite(decoded)) return false;
        try {
            int scale = -q;
            BigDecimal roundedOriginal = BigDecimal.valueOf(original).setScale(scale, RoundingMode.HALF_UP);
            BigDecimal roundedDecoded = BigDecimal.valueOf(decoded).setScale(scale, RoundingMode.HALF_UP);
            return roundedOriginal.compareTo(roundedDecoded) == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void recordRawRelativeError(double original, double decoded) {
        double denominator = Math.abs(original);
        if (denominator == 0 || !Double.isFinite(original) || !Double.isFinite(decoded)) return;
        double relativeError = Math.abs(original - decoded) / denominator;
        rawRelativeErrorSum += relativeError;
        rawRelativeErrorMax = Math.max(rawRelativeErrorMax, relativeError);
        rawRelativeErrorSamples++;
    }

    private void recordPrecisionRelativeError(double original, double decoded, int q) {
        if (q == Integer.MIN_VALUE || !Double.isFinite(original) || !Double.isFinite(decoded)) return;
        try {
            double roundedOriginal = BigDecimal.valueOf(original).setScale(-q, RoundingMode.HALF_UP).doubleValue();
            double roundedDecoded = BigDecimal.valueOf(decoded).setScale(-q, RoundingMode.HALF_UP).doubleValue();
            double denominator = Math.abs(roundedOriginal);
            if (denominator == 0) return;
            double relativeError = Math.abs(roundedOriginal - roundedDecoded) / denominator;
            precisionRelativeErrorSum += relativeError;
            precisionRelativeErrorMax = Math.max(precisionRelativeErrorMax, relativeError);
            precisionRelativeErrorSamples++;
        } catch (Exception ignored) {
        }
    }

    private void putMetrics(double decompSpeed) {
        info.put("verification_mode", verificationMode.getConfigName());
        info.put("bounded_error_count", String.valueOf(boundedErrorCount));
        info.put("decomp_total", String.valueOf(total));
        info.put("decomp_time_ms", String.valueOf(finish_time));
        info.put("decomp_speed", result_format(decompSpeed));
        info.put("raw_error_count", String.valueOf(rawErrorCount));
        info.put("raw_error_rate", total == 0 ? "0" : String.valueOf((double) rawErrorCount / total));
        info.put("raw_relative_error_sum", String.valueOf(rawRelativeErrorSum));
        info.put("raw_relative_error_samples", String.valueOf(rawRelativeErrorSamples));
        info.put("raw_mean_relative_error", rawRelativeErrorSamples == 0 ? "0"
                : String.valueOf(rawRelativeErrorSum / rawRelativeErrorSamples));
        info.put("raw_max_relative_error", String.valueOf(rawRelativeErrorMax));
        info.put("precision_error_count", String.valueOf(precisionErrorCount));
        info.put("precision_error_rate", total == 0 ? "0" : String.valueOf((double) precisionErrorCount / total));
        info.put("precision_relative_error_sum", String.valueOf(precisionRelativeErrorSum));
        info.put("precision_relative_error_samples", String.valueOf(precisionRelativeErrorSamples));
        info.put("precision_mean_relative_error", precisionRelativeErrorSamples == 0 ? "0"
                : String.valueOf(precisionRelativeErrorSum / precisionRelativeErrorSamples));
        info.put("precision_max_relative_error", String.valueOf(precisionRelativeErrorMax));
    }

    private void standaloneDecompress() {
        while (true) {
            try {
                long start_time = System.nanoTime();
                double dec_v = decoder.decodeDouble();
                long end_time = System.nanoTime();
                finish_time += (double) (end_time - start_time) / 1000000;
                total++;
            } catch (Exception e) {
                break;
            }
        }

        double decomp_speed = (double) (total * dataType.getSize() / 8) / finish_time;

        System.out.println(algorithm_name + " standalone decompress success! Total " + total
                + " values. Finish time is " + result_format(finish_time) + "ms");
        info.put("decomp_total", result_format(total));
        info.put("decomp_speed", result_format(decomp_speed));
    }
}
