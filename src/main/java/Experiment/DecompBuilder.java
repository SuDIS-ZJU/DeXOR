package Experiment;

import algorithms.AlgorithmsManager;
import algorithms.Decoder;
import enums.DataTypeEnums;
import utils.TableStreamer;

import java.util.HashMap;
import java.util.Map;

public class DecompBuilder {
    private static final double[] EPS = new double[]{1, 1e-1, 1e-2, 1e-3, 1e-4, 1e-5, 1e-6, 1e-7, 1e-8, 1e-9, 1e-10, 1e-11, 1e-12,
            1e-13, 1e-14, 1e-15, 1e-16, 1e-17, 1e-18, 1e-19, 1e-20, 1e-21, 1e-22, 1e-23};


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
        while (true) {
            try {
                double v = table.getDouble(1);
                total++;

                // debug
                if (table_name.equals("Air-pressure") && total == 854) {
                    int k = 111;
                }
                int place = getDecimalPlace(v);
                double eps = EPS[place];
                long start_time = System.nanoTime();
                double dec_v = decoder.decodeDouble();
                long end_time = System.nanoTime();
                finish_time += (double) (end_time - start_time) / 1000000; // convert to ms
                if (Math.abs(v - dec_v) >= eps) {
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
