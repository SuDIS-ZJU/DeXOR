package Experiment;

import algorithms.AlgorithmsManager;
import algorithms.Encoder;
import enums.DataTypeEnums;
import utils.TableStreamer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompBuilder {

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
    Map<String, String> info = new HashMap<>();


    public CompBuilder(DataTypeEnums dataType, String algorithm_name, String table_name, String table_path, String output_path, String config_path) throws Exception {
        this.dataType = dataType;
        this.algorithmName = algorithm_name;
        this.tablePath = table_path;
        this.tableName = table_name;
        this.outputPath = output_path;
        this.configPath = config_path;
        String config = null;
        if (!config_path.isEmpty()) config = seekConfig();
        if (config_path != null && config == null)
            this.encoder = AlgorithmsManager.getEncoder(dataType.getType(), algorithm_name, outputPath);
        else this.encoder = AlgorithmsManager.getEncoder(dataType.getType(), algorithm_name, outputPath, config);
        this.table = new TableStreamer(tablePath);
    }

    public String seekConfig() {
        Pattern pattern = Pattern.compile(algorithmName + "\\{([^}]*)\\}");
        String line;

        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
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
        if (!use_log) return;
        double comp_speed = (double) (total * dataType.getSize() / 8) / finish_time;
        double comp_bits = bits / total;

        System.out.println(algorithmName + " compress \"" + tableName + "\" success! Total " + total
                + " values stored in \"" + outputPath + "\". Finish time is "
                + result_format(finish_time) + "ms and average bits is " + result_format(comp_bits));

        info.put("total", result_format(total));
        info.put("comp_speed", result_format(comp_speed));
        info.put("comp_bits", result_format(comp_bits));

        Map<String, Double> meta = encoder.getMeta();
        for (String key : meta.keySet()) {
            info.put(key, result_format(meta.get(key)));
        }
    }


    //todo add other types
    public void compress() {
        if (dataType.equals(DataTypeEnums.DOUBLE)) {
            compressDouble();
        }
        print_trace();
    }

    protected void compressDouble() {
        while (true) {
            try {
                double v = table.getDouble(1);
                total++;

                // debug
                if (tableName.equals("Air-sensor") && total == 1) {
                    int k = 111;
                }

                long start_time = System.nanoTime();
                bits += encoder.encode(v);
                long end_time = System.nanoTime();
                finish_time += (double) (end_time - start_time) / 1000000; // convert to ms
                table.next();
            } catch (Exception e) {
                // for batch
                long start_time = System.nanoTime();
                int residual = encoder.close();
                long end_time = System.nanoTime();

                if (residual > 0) {
                    bits += residual;
                    finish_time += (double) (end_time - start_time) / 1000000;
                }

                encoder.flush();
                break;
            }
        }
    }

}
