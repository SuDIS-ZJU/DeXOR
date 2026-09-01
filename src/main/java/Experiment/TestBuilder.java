package Experiment;

import enums.AlgorithmEnums;
import enums.DataTypeEnums;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import utils.TableStreamer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class TestBuilder {
    private final DataTypeEnums data_type;
    private final String data_path;
    private final String store_path;
    private final String config_path;

    private final String result_path;

    private final List<String> algorithms;
    private int decimalPlaces = -1;
    private VerificationMode verificationMode = VerificationMode.BITWISE;
    private static int failedDecimalPlaces = 2;

    private Map<String, List<List<String>>> results;
    private int index = 0;

    public static void setFailedDecimalPlaces(int value) {
        failedDecimalPlaces = value;
    }

    public void setVerify(boolean verify) {
        this.verificationMode = verify ? VerificationMode.BITWISE : VerificationMode.NONE;
    }

    public void setVerificationMode(VerificationMode verificationMode) {
        this.verificationMode = verificationMode == null ? VerificationMode.BITWISE : verificationMode;
    }

    public TestBuilder(DataTypeEnums data_type, String data_path, String store_path, String result_path,String config_path, AlgorithmEnums[] test_algorithms) {
        this(data_type, data_path, store_path, result_path, config_path, test_algorithms, -1);
    }
    
    public TestBuilder(DataTypeEnums data_type, String data_path, String store_path, String result_path,String config_path, AlgorithmEnums[] test_algorithms, int decimalPlaces) {
        this.data_type = data_type;
        this.data_path = data_path;
        this.store_path = store_path;
        this.result_path = result_path;
        this.config_path = config_path;
        this.decimalPlaces = decimalPlaces;
        algorithms = new ArrayList<>();
        for (AlgorithmEnums algorithmEnum : test_algorithms) {
            algorithms.add(algorithmEnum.getName());
        }
        this.init_result();
    }

    public void init_result() {
        results = new HashMap<>();
        for (String name : algorithms) {
            List<List<String>> result = new ArrayList<>();
            results.put(name, result);
        }
    }

    private void createPath(String path){
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public void test_comp() {
        test_all();
    }

    public void test_decomp() {
        test_all();
    }
    
    public void test_all() {
        createPath(store_path);
        for (String name : algorithms) {
            File folder = new File(store_path + "/" + name);
            folder.mkdir();
        }
        dfs_all(new File(data_path));
    }

    public void write_results() {
        createPath(result_path);
        for (String name : algorithms) {
            to_csv(results.get(name), result_path + "/" + name + ".csv");
        }
    }

    private void dfs_all(File dir) { // N datasets
        if (!dir.isDirectory()) {
            if (dir.getName().endsWith(".csv")) {
                String name = dir.getName();
                name = name.substring(0, name.length() - 4);
                process_dataset(name, dir.getAbsolutePath());
            }
            return;
        }
        File[] folder = dir.listFiles();

        if (folder != null) {
            for (File file : folder) {
                String name = file.getName();
                if (file.isDirectory()) dfs_all(file);
                else if (name.endsWith(".csv")) {
                    name = name.substring(0, name.length() - 4);
                    process_dataset(name, file.getAbsolutePath());
                }
            }
        }
    }
    
    private void process_dataset(String dataset_name, String table_path) {
        int[] numericCols;
        try {
            TableStreamer tempTable = new TableStreamer(table_path);
            numericCols = tempTable.getNumericColumnIndices();
        } catch (Exception e) {
            numericCols = new int[]{1};
        }
        
        for (String algorithm_name : algorithms) {
            Map<String, String> fullInfo = new LinkedHashMap<>();
            fullInfo.put("Dataset", dataset_name);
            
            // Compression
            try {
                String output_path = store_path + "/" + algorithm_name + "/" + dataset_name + "." + algorithm_name.toLowerCase();
                int actualDecimalPlaces = effectiveDecimalPlaces(algorithm_name);
                CompBuilder comp = new CompBuilder(data_type, algorithm_name, dataset_name, table_path, output_path, config_path, numericCols, actualDecimalPlaces);
                comp.compress();
                Map<String, String> comp_info = comp.getInfo();
                fullInfo.putAll(comp_info);
            } catch (Exception ignored) {
            }
            
            // Decompression
            long totalValues = 0;
            double totalDecompTimeMs = 0;
            boolean hasError = false;
            String errorMsg = "";
            long boundedErrorCount = 0;
            long rawErrorCount = 0;
            long precisionErrorCount = 0;
            double rawRelativeErrorSum = 0;
            long rawRelativeErrorSamples = 0;
            double rawRelativeErrorMax = 0;
            double precisionRelativeErrorSum = 0;
            long precisionRelativeErrorSamples = 0;
            double precisionRelativeErrorMax = 0;
            
            for (int colIdx = 0; colIdx < numericCols.length; colIdx++) {
                int col = numericCols[colIdx];
                String colName;
                try {
                    TableStreamer tempTable = new TableStreamer(table_path);
                    colName = tempTable.getColumnName(col);
                } catch (Exception e) {
                    colName = "col" + col;
                }
                
                try {
                    int actualDecimalPlaces = effectiveDecimalPlaces(algorithm_name);
                    String colInputPath = store_path + "/" + algorithm_name + "/" + dataset_name + "_" + colName + "." + algorithm_name.toLowerCase();
                    DecompBuilder decomp = new DecompBuilder(data_type, algorithm_name, dataset_name, table_path, colInputPath, config_path, col, colName, actualDecimalPlaces);
                    decomp.setVerificationMode(verificationMode);
                    decomp.test_decompress();
                    Map<String, String> decomp_info = decomp.getInfo();
                    
                    if (decomp_info.containsKey("error")) {
                        hasError = true;
                        errorMsg = decomp_info.get("error");
                    }
                    totalValues += parseLong(decomp_info, "decomp_total");
                    totalDecompTimeMs += parseDouble(decomp_info, "decomp_time_ms");
                    boundedErrorCount += parseLong(decomp_info, "bounded_error_count");
                    rawErrorCount += parseLong(decomp_info, "raw_error_count");
                    precisionErrorCount += parseLong(decomp_info, "precision_error_count");
                    rawRelativeErrorSum += parseDouble(decomp_info, "raw_relative_error_sum");
                    rawRelativeErrorSamples += parseLong(decomp_info, "raw_relative_error_samples");
                    rawRelativeErrorMax = Math.max(rawRelativeErrorMax,
                            parseDouble(decomp_info, "raw_max_relative_error"));
                    precisionRelativeErrorSum += parseDouble(decomp_info, "precision_relative_error_sum");
                    precisionRelativeErrorSamples += parseLong(decomp_info, "precision_relative_error_samples");
                    precisionRelativeErrorMax = Math.max(precisionRelativeErrorMax,
                            parseDouble(decomp_info, "precision_max_relative_error"));
                } catch (Exception ignored) {
                }
            }

            double decompSpeed = totalDecompTimeMs == 0 ? 0
                    : (double) (totalValues * data_type.getSize() / 8) / totalDecompTimeMs;
            long expectedValues = parseLong(fullInfo, "total");
            if (totalValues != expectedValues) {
                hasError = true;
                if (!errorMsg.isEmpty()) {
                    errorMsg += "; ";
                }
                errorMsg += "decoded value count mismatch: expected=" + expectedValues
                        + ", actual=" + totalValues;
            }
            fullInfo.put("decomp_total", String.valueOf(totalValues));
            fullInfo.put("decomp_speed", result_format(decompSpeed));
            fullInfo.put("verification_mode", verificationMode.getConfigName());
            int validationDecimalPlaces = effectiveDecimalPlaces(algorithm_name);
            long validationErrorCount = validationDecimalPlaces >= 0
                    ? boundedErrorCount
                    : (verificationMode == VerificationMode.PRECISION
                    ? precisionErrorCount : rawErrorCount);
            fullInfo.put("validation_decimal_places", String.valueOf(validationDecimalPlaces));
            fullInfo.put("validation_type", validationDecimalPlaces >= 0
                    ? "absolute_error" : verificationMode.getConfigName());
            fullInfo.put("bounded_error_count", String.valueOf(boundedErrorCount));
            fullInfo.put("bounded_error_rate", totalValues == 0 ? "0"
                    : String.valueOf((double) boundedErrorCount / totalValues));
            fullInfo.put("validation_error_count", String.valueOf(validationErrorCount));
            fullInfo.put("validation_error_rate", totalValues == 0 ? "0"
                    : String.valueOf((double) validationErrorCount / totalValues));
            fullInfo.put("raw_error_count", String.valueOf(rawErrorCount));
            fullInfo.put("raw_error_rate", totalValues == 0 ? "0"
                    : String.valueOf((double) rawErrorCount / totalValues));
            fullInfo.put("raw_relative_error_sum", String.valueOf(rawRelativeErrorSum));
            fullInfo.put("raw_relative_error_samples", String.valueOf(rawRelativeErrorSamples));
            fullInfo.put("raw_mean_relative_error", rawRelativeErrorSamples == 0 ? "0"
                    : String.valueOf(rawRelativeErrorSum / rawRelativeErrorSamples));
            fullInfo.put("raw_max_relative_error", String.valueOf(rawRelativeErrorMax));
            fullInfo.put("precision_error_count", String.valueOf(precisionErrorCount));
            fullInfo.put("precision_error_rate", totalValues == 0 ? "0"
                    : String.valueOf((double) precisionErrorCount / totalValues));
            fullInfo.put("precision_relative_error_sum", String.valueOf(precisionRelativeErrorSum));
            fullInfo.put("precision_relative_error_samples", String.valueOf(precisionRelativeErrorSamples));
            fullInfo.put("precision_mean_relative_error", precisionRelativeErrorSamples == 0 ? "0"
                    : String.valueOf(precisionRelativeErrorSum / precisionRelativeErrorSamples));
            fullInfo.put("precision_max_relative_error", String.valueOf(precisionRelativeErrorMax));
            fullInfo.put("status", hasError ? "FAILED: " + errorMsg : "SUCCESS");
            
            List<List<String>> result = results.get(algorithm_name);
            if (result.isEmpty()) {
                List<String> header = new ArrayList<>(fullInfo.keySet());
                result.add(header);
            }
            List<String> row = new ArrayList<>();
            for (String key : result.get(0)) {
                row.add(fullInfo.containsKey(key) ? fullInfo.get(key) : "");
            }
            result.add(row);
        }
    }
    
    private static Set<String> LOSSY_ONLY_ALGORITHMS = new HashSet<>();

    private long parseLong(Map<String, String> info, String key) {
        try {
            return (long) Double.parseDouble(info.getOrDefault(key, "0"));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double parseDouble(Map<String, String> info, String key) {
        try {
            return Double.parseDouble(info.getOrDefault(key, "0"));
        } catch (Exception ignored) {
            return 0;
        }
    }
    
    public static void setLossyOnlyAlgorithms(Set<String> algorithms) {
        LOSSY_ONLY_ALGORITHMS = algorithms;
    }

    private int effectiveDecimalPlaces(String algorithmName) {
        if (!LOSSY_ONLY_ALGORITHMS.contains(algorithmName)) {
            return -1;
        }
        return decimalPlaces < 0 ? failedDecimalPlaces : decimalPlaces;
    }
    
    private String buildConfig(String algorithmName, int decimalPlaces) {
        if (LOSSY_ONLY_ALGORITHMS.contains(algorithmName)) {
            if (decimalPlaces < 0) {
                decimalPlaces = failedDecimalPlaces;
            }
            return "decimal_places:" + decimalPlaces;
        }
        return "";
    }
    
    private int getDecimalPlacesForDecomp(String algorithmName, int decimalPlaces) {
        if (LOSSY_ONLY_ALGORITHMS.contains(algorithmName)) {
            if (decimalPlaces < 0) {
                decimalPlaces = failedDecimalPlaces;
            }
        }
        return decimalPlaces;
    }

    private void init_result_header(List<List<String>> result, Map<String, String> info) {
        List<String> row = new ArrayList<>();
        row.add("Dataset");
        for (String key : info.keySet()) {
            row.add(key);
        }
        result.add(row);
    }

    private void comp_dataset(String dataset_name, String table_path) { // 1 dataset N algorithms
        int[] numericCols;
        try {
            TableStreamer tempTable = new TableStreamer(table_path);
            numericCols = tempTable.getNumericColumnIndices();
        } catch (Exception e) {
            numericCols = new int[]{1};
        }
        
        for (String algorithm_name : algorithms) {
            try {
                String output_path = store_path + "/" + algorithm_name + "/" + dataset_name + "." + algorithm_name.toLowerCase();
                CompBuilder comp = new CompBuilder(data_type, algorithm_name, dataset_name, table_path, output_path, config_path, numericCols);
                comp.compress();
                Map<String, String> comp_info = comp.getInfo();
                List<List<String>> result = results.get(algorithm_name);
                if (result.isEmpty()) init_result_header(result, comp_info);
                List<String> row = new ArrayList<>();
                row.add(dataset_name);
                for (String key : comp_info.keySet()) {
                    row.add(comp_info.get(key));
                }
                result.add(row);
            } catch (Exception ignored) {
            }
        }
    }

    private void decomp_dataset(String dataset_name, String table_path) { // 1 dataset N algorithms
        index++;
        
        int[] numericCols;
        try {
            TableStreamer tempTable = new TableStreamer(table_path);
            numericCols = tempTable.getNumericColumnIndices();
        } catch (Exception e) {
            numericCols = new int[]{1};
        }
        
        for (String algorithm_name : algorithms) {
            long totalValues = 0;
            double totalTime = 0;
            boolean hasError = false;
            String errorMsg = "";
            
            for (int colIdx = 0; colIdx < numericCols.length; colIdx++) {
                int col = numericCols[colIdx];
                String colName;
                try {
                    TableStreamer tempTable = new TableStreamer(table_path);
                    colName = tempTable.getColumnName(col);
                } catch (Exception e) {
                    colName = "col" + col;
                }
                
                try {
                    String colInputPath = store_path + "/" + algorithm_name + "/" + dataset_name + "_" + colName + "." + algorithm_name.toLowerCase();
                    DecompBuilder decomp = new DecompBuilder(data_type, algorithm_name, dataset_name, table_path, colInputPath, config_path, col, colName);
                    decomp.setVerificationMode(verificationMode);
                    decomp.test_decompress();
                    Map<String, String> decomp_info = decomp.getInfo();
                    
                    if (decomp_info.containsKey("error")) {
                        hasError = true;
                        errorMsg = decomp_info.get("error");
                    } else {
                        try {
                            totalValues += Long.parseLong(decomp_info.get("decomp_total"));
                            totalTime += Double.parseDouble(decomp_info.get("decomp_speed"));
                        } catch (Exception ex) {
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            
            List<List<String>> result = results.get(algorithm_name);
            if (index == 1) {
                List<String> header = new ArrayList<>();
                header.add("Dataset");
                header.add("total");
                header.add("decomp_speed");
                header.add("status");
                result.add(header);
            }
            
            List<String> row;
            if (index <= result.size()) row = result.get(index);
            else {
                row = new ArrayList<>();
                result.add(row);
            }
            row.add(dataset_name);
            row.add(String.valueOf(totalValues));
            row.add(result_format(totalTime));
            row.add(hasError ? "FAILED: " + errorMsg : "SUCCESS");
        }
    }

    private void to_csv(List<List<String>> data, String path) {
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(path), CSVFormat.DEFAULT)) {
            for (List<String> row : data) {
                csvPrinter.printRecord(row);
            }
            System.out.println("results save in " + path);
        } catch (IOException e) {
        //            e.printStackTrace();
        }
    }
    
    private String result_format(double v) {
        return String.format("%.2f", v);
    }
}
