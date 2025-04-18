package org.example;

import Experiment.TestBuilder;
import enums.AlgorithmEnums;
import enums.DataTypeEnums;

public class Main {
    public static void main(String[] args) {
        String data_path = "./datasets/Overall";
        String store_path = "./storage";
        String result_path = "./results";
        AlgorithmEnums[] methods = new AlgorithmEnums[]{AlgorithmEnums.DXOR};
//        AlgorithmEnums[] methods = AlgorithmEnums.values();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-in":
                    data_path = args[++i];
                    break;
                case "-out":
                    store_path = args[++i];
                    break;
                case "-log":
                    result_path = args[++i];
                    break;
                case "-m":
                    methods = new AlgorithmEnums[]{};
                    while(++i < args.length){
                        String name = args[i];
                        AlgorithmEnums alg = AlgorithmEnums.CheckName(name);
                        if(alg==null){
                            --i;
                            break;
                        }
                    }

                    break;
            }
        }

        TestBuilder t1 = new TestBuilder(DataTypeEnums.DOUBLE, data_path, store_path, result_path, methods);
        t1.test_comp();
        t1.test_decomp();
        t1.write_results();
    }
}