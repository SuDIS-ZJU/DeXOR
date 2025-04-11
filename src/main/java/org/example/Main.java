package org.example;

import Experiment.TestBuilder;
import enums.AlgorithmEnums;
import enums.DataTypeEnums;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String data_path = "./datasets/DataFromElf"; // DataFromElf test continuous random
        String store_path = "./storage";
        String result_path = "./results";
        AlgorithmEnums[] methods = new AlgorithmEnums[]{AlgorithmEnums.ATDP};
//        AlgorithmEnums[] methods = AlgorithmEnums.values();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-in")) {
                data_path = args[++i];
            } else if (args[i].equals("-out")) {
                store_path = args[++i];
            } else if (args[i].equals("-log")) {
                result_path = args[++i];
            } else if (args[i].equals("-m")) {
                try {
                    String name = args[++i];
                    AlgorithmEnums alg = AlgorithmEnums.CheckName(name);
                    methods = new AlgorithmEnums[]{alg};
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        TestBuilder t1 = new TestBuilder(DataTypeEnums.DOUBLE, data_path, store_path, result_path, methods);
        t1.test_comp();
        t1.test_decomp();
        t1.write_results();
    }
}