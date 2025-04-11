package Experiment;

import algorithms.AlgorithmsManager;
import algorithms.Decoder;
import enums.DataTypeEnums;
import utils.TableStreamer;

import java.util.HashMap;
import java.util.Map;

public class DecompBuilder {
    private final Decoder decoder;
    private final TableStreamer table;

    private final DataTypeEnums dataType;
    private final String algorithm_name;

    private final String input_path;

    private String table_name;


    private long total = 0;
    private long error_id = 0;

    private double finish_time = 0;
    private Map<String, String> info = new HashMap<>();

    public Map<String, String> getInfo() {
        return info;
    }

    public DecompBuilder(DataTypeEnums dataType, String algorithm_name, String table_name, String table_path, String input_path) throws Exception {
        this.dataType = dataType;
        this.algorithm_name = algorithm_name;
        this.table_name = table_name;
        this.input_path = input_path;
        this.decoder = AlgorithmsManager.getDecoder(dataType.getType(), algorithm_name, input_path);
        this.table = new TableStreamer(table_path);
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

    private void test_decompressDouble() {
        while (true) {
            try {
                double v = table.getDouble(1);
                total++;

                // debug
//                if (table_name.equals("Food-price.csv") && total == 506) {
//                    int k = 111;
//                }

                long start_time = System.nanoTime();
                double dec_v = decoder.decodeDouble();
                long end_time = System.nanoTime();
                finish_time += (double) (end_time - start_time) / 1000000; // convert to ms
                if (Math.abs(v - dec_v) > 1e-5) {
                    error_id = total;
                    System.out.println("Error happened at " + error_id + " with v=" + v + " in " + algorithm_name + " and decompress result is " + dec_v);
                    break;
                }
                table.next();
            } catch (Exception e) {
                break;
            }
        }
        double decomp_speed = (double) (total * dataType.getSize() / 8) / finish_time;

        if (error_id > 0) {
            System.out.println(algorithm_name + " decompress \"" + input_path + "\" failed!");
            return;
        } else {
            System.out.println(algorithm_name + " decompress \"" + input_path + "\" success! Total " + total
                    + " values. Finish time is "
                    + result_format(finish_time) + "ms");
        }

        info.put("decomp_speed", result_format(decomp_speed));
    }
}
