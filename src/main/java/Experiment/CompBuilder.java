package Experiment;

import algorithms.AlgorithmsManager;
import algorithms.Encoder;
import enums.DataTypeEnums;
import utils.TableStreamer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompBuilder {
    private static final double[] EPS = new double[]{1, 1e-1, 1e-2, 1e-3, 1e-4, 1e-5, 1e-6, 1e-7, 1e-8, 1e-9, 1e-10, 1e-11, 1e-12,
            1e-13, 1e-14, 1e-15, 1e-16, 1e-17, 1e-18, 1e-19, 1e-20, 1e-21, 1e-22, 1e-23};
    private Encoder encoder;
    private TableStreamer table;

    private DataTypeEnums dataType;
    private String algorithmName;
    private String outputPath;
    private String tablePath;
    private String configPath;
    private String tableName;

    private long total = 0;
    private double bits = 0;

    private boolean use_log = true;

    private double finish_time = 0;
    Map<String, String> info = new LinkedHashMap<>();
    
    private int[] numericCols;
    private int decimalPlaces = -1;


    public CompBuilder(DataTypeEnums dataType, String algorithm_name, String table_name, String table_path, String output_path, String config_path) throws Exception {
        this(dataType, algorithm_name, table_name, table_path, output_path, config_path, new int[]{1});
    }
    
    public CompBuilder(DataTypeEnums dataType, String algorithm_name, String table_name, String table_path, String output_path, String config_path, int[] numericCols) throws Exception {
        this(dataType, algorithm_name, table_name, table_path, output_path, config_path, numericCols, -1);
    }
    
    public CompBuilder(DataTypeEnums dataType, String algorithm_name, String table_name, String table_path, String output_path, String config_path, int[] numericCols, int decimalPlaces) throws Exception {
        this.dataType = dataType;
        this.algorithmName = algorithm_name;
        this.tablePath = table_path;
        this.tableName = table_name;
        this.outputPath = output_path;
        this.configPath = config_path;
        this.numericCols = numericCols;
        this.decimalPlaces = decimalPlaces;
        
        String config = null;
        if (configPath != null && !configPath.isEmpty()) config = seekConfig();
        
        config = withDecimalPlaces(config, decimalPlaces);
        
        if (config != null && !config.isEmpty())
            this.encoder = AlgorithmsManager.getEncoder(dataType.getType(), algorithm_name, outputPath, config);
        else 
            this.encoder = AlgorithmsManager.getEncoder(dataType.getType(), algorithm_name, outputPath);
        this.table = new TableStreamer(tablePath);
    }

    public String seekConfig() {
        Pattern algoPattern = Pattern.compile(algorithmName + "\\{([^}]*)\\}");
        Pattern globalPattern = Pattern.compile("global\\{([^}]*)\\}");
        String line;
        String algoConfig = null;
        String globalConfig = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
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

    public Map<String, String> getInfo() {
        return info;
    }

    // result log
    public void setLog(boolean use_Log) {
        this.use_log = use_Log;
    }

    protected String result_format(double v) {
        return String.format("%.2f", v);
    }

    protected void print_trace() {
        // Global stats already handled in compressDouble()
    }


    //todo add other types
    public void compress() {
        if (dataType.equals(DataTypeEnums.DOUBLE)) {
            compressDouble();
        }
        print_trace();
    }

    protected void compressDouble() {
        if (numericCols == null || numericCols.length == 0) {
            numericCols = table.getNumericColumnIndices();
        }
        String algorithmSuffix = algorithmName.toLowerCase();
        
        long totalValues = 0;
        long totalBits = 0;
        double totalTime = 0;
        Map<String, Double> metricTotals = new LinkedHashMap<>();
        
        for (int colIdx = 0; colIdx < numericCols.length; colIdx++) {
            int col = numericCols[colIdx];
            String colName = table.getColumnName(col);
            
            String colOutputPath = outputPath.replaceAll("\\.[^.]+$", "_" + colName + "." + algorithmSuffix);
            
            Encoder colEncoder;
            try {
                String config = null;
                if (configPath != null && !configPath.isEmpty()) config = seekConfig();
                config = withDecimalPlaces(config, this.decimalPlaces);
                colEncoder = AlgorithmsManager.getEncoder(dataType.getType(), algorithmName, colOutputPath, config);
            } catch (Exception e) {
                continue;
            }
            
            long colTotal = 0;
            long colBits = 0;
            double colFinishTime = 0;
            
            try {
                table.reset();
            } catch (Exception e) {
                break;
            }
            
            while (true) {
                double v;
                boolean valueAvailable = true;
                try {
                    v = table.getDouble(col);
                } catch (Exception invalidValue) {
                    try {
                        table.next();
                        continue;
                    } catch (Exception eof) {
                        v = 0;
                        valueAvailable = false;
                    }
                }

                if (valueAvailable) {
                    colTotal++;
                    long start_time = System.nanoTime();
                    colBits += colEncoder.encode(v);
                    long end_time = System.nanoTime();
                    colFinishTime += (double) (end_time - start_time) / 1000000;
                }

                try {
                    table.next();
                } catch (Exception eof) {
                    long start_time = System.nanoTime();
                    int residual = colEncoder.close();
                    long end_time = System.nanoTime();

                    if (residual > 0) {
                        colBits += residual;
                        colFinishTime += (double) (end_time - start_time) / 1000000;
                    }

                    colEncoder.flush();
                    for (Map.Entry<String, Double> entry : colEncoder.getMeta().entrySet()) {
                        if (entry.getKey().startsWith("metric.")) {
                            metricTotals.merge(entry.getKey().substring("metric.".length()),
                                    entry.getValue(), Double::sum);
                        }
                    }
                    break;
                }
            }
            
            if (colTotal > 0) {
                totalValues += colTotal;
                totalBits += colBits;
                totalTime += colFinishTime;
                
                System.out.println(algorithmName + " compress \"" + tableName + "." + colName + "\" success! Total " + colTotal
                        + " values. Finish time is " + result_format(colFinishTime) + "ms and average bits is " + result_format((double)colBits / colTotal));
            }
        }
        
        if (totalValues > 0) {
            double comp_speed = (double) (totalValues * dataType.getSize() / 8) / totalTime;
            double comp_bits = (double) totalBits / totalValues;
            
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            System.out.println(algorithmName + " compress \"" + tableName + "\" success! Total " + totalValues
                    + " values. Finish time is " + result_format(totalTime) + "ms and average bits is " + result_format(comp_bits));
            
            info.put("total", result_format(totalValues));
            info.put("comp_speed", result_format(comp_speed));
            info.put("comp_bits", result_format(comp_bits));
            info.put("memory_used", result_format(usedMemory / 1024.0 / 1024.0));
            info.put("memory_max", result_format(maxMemory / 1024.0 / 1024.0));
            for (Map.Entry<String, Double> entry : metricTotals.entrySet()) {
                info.put(entry.getKey(), String.valueOf(entry.getValue().longValue()));
            }
            double exceptionCount = metricTotals.getOrDefault("exception_count", 0.0);
            double exceptionBits = metricTotals.getOrDefault("exception_payload_bits", 0.0);
            if (exceptionCount > 0) {
                info.put("exception_rate", String.valueOf(exceptionCount / totalValues));
                info.put("exception_mean_payload_bits", String.valueOf(exceptionBits / exceptionCount));
            }
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

}
