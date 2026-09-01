package org.example;

import Experiment.DecompBuilder;
import Experiment.TestBuilder;
import Experiment.VerificationMode;
//import Experiment.TsfileTestBuilder;
import algorithms.AlgorithmsManager;
import algorithms.Decoder;
import enums.AlgorithmEnums;
import enums.DataTypeEnums;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class Main {

    private static Map<String, String> globalConfig = new HashMap<>();
    private static Map<String, String> algorithmConfigs = new HashMap<>();
    private static Set<String> lossyOnlyAlgorithms = new HashSet<>();

    public static void main(String[] args) {
        String data_path = "./datasets/test"; ///
        String store_path = "./storage";
        String result_path = "./results";
        AlgorithmEnums[] methods = new AlgorithmEnums[]{AlgorithmEnums.DeXOR};
        String config_path = "./config.txt";

        boolean standalone = false;
        String decompFile = "";
        VerificationMode verificationMode = VerificationMode.BITWISE;
        String cliVerificationMode = null;
        int decimalPlaces = -1;
        Integer cliDecimalPlaces = null;

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
                case "-config":
                    config_path = args[++i];
                    break;
                case "-m":
                    Set<AlgorithmEnums> set = new HashSet<>();
                    while(++i < args.length){
                        String name = args[i];
                        AlgorithmEnums alg = AlgorithmEnums.CheckName(name);
                        if(alg==null){
                            --i;
                            break;
                        }
                        set.add(alg);
                    }
                    if(!set.isEmpty())methods = set.toArray(new AlgorithmEnums[0]);
                    break;
                case "-decompress":
                case "-d":
                    standalone = true;
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        decompFile = args[++i];
                    }
                    break;
                case "-verify":
                    cliVerificationMode = args[++i];
                    break;
                case "-verify-mode":
                    cliVerificationMode = args[++i];
                    break;
                case "-lossy":
                    cliDecimalPlaces = Integer.parseInt(args[++i]);
                    break;
            }
        }

        globalConfig.clear();
        algorithmConfigs.clear();
        lossyOnlyAlgorithms.clear();
        loadConfig(config_path);

        if (globalConfig.containsKey("decimal_places")) {
            try {
                decimalPlaces = Integer.parseInt(globalConfig.get("decimal_places"));
            } catch (NumberFormatException e) {
                decimalPlaces = -1;
            }
        }
        if (cliDecimalPlaces != null) {
            decimalPlaces = cliDecimalPlaces;
        }

        if (globalConfig.containsKey("verification_mode")) {
            verificationMode = VerificationMode.parse(globalConfig.get("verification_mode"));
        } else if (globalConfig.containsKey("verify")) {
            verificationMode = VerificationMode.parse(globalConfig.get("verify"));
        }
        if (cliVerificationMode != null) {
            verificationMode = VerificationMode.parse(cliVerificationMode);
        }

        String fallbackDecimalPlaces = globalConfig.containsKey("fallback_decimal_places")
                ? globalConfig.get("fallback_decimal_places")
                : globalConfig.get("failed_decimal_places");
        if (fallbackDecimalPlaces != null) {
            try {
                TestBuilder.setFailedDecimalPlaces(Integer.parseInt(fallbackDecimalPlaces));
            } catch (NumberFormatException e) {
            }
        }
        TestBuilder.setLossyOnlyAlgorithms(lossyOnlyAlgorithms);

        if (standalone) {
            standaloneDecompress(decompFile, methods, verificationMode, decimalPlaces, config_path);
        } else {
            TestBuilder t1 = new TestBuilder(DataTypeEnums.DOUBLE, data_path, store_path, result_path, config_path, methods, decimalPlaces);
            t1.setVerificationMode(verificationMode);
            t1.test_all();
            t1.write_results();
        }
    }
    
    private static void standaloneDecompress(String decompFile, AlgorithmEnums[] methods, VerificationMode verificationMode, int decimalPlaces, String configPath) {
        for (AlgorithmEnums method : methods) {
            try {
                String inputPath = decompFile.isEmpty() ? "" : decompFile;
                if (inputPath.isEmpty()) {
                    System.err.println("Please specify the file to decompress with -d <file>");
                    return;
                }
                
                String config = buildConfig(method.getName(), decimalPlaces);
                DecompBuilder decomp = new DecompBuilder(DataTypeEnums.DOUBLE, method.getName(), inputPath, config);
                decomp.setVerificationMode(verificationMode);
                decomp.setDecimalPlaces(decimalPlaces);
                decomp.test_decompress();
            } catch (Exception e) {
                System.err.println("Error decompressing with " + method.getName() + ": " + e.getMessage());
            }
        }
    }
     
    private static String buildConfig(String algorithmName, int decimalPlaces) {
        if (algorithmConfigs.containsKey(algorithmName)) {
            String config = algorithmConfigs.get(algorithmName);
            if (decimalPlaces > 0) {
                config = "epsilon:-" + decimalPlaces + (config.isEmpty() ? "" : "," + config);
            }
            return config;
        }
        if (algorithmName.equalsIgnoreCase("DeXORPlus") && decimalPlaces > 0) {
            return "epsilon:-" + decimalPlaces;
        }
        return "";
    }
    
    private static void loadConfig(String configPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                if (line.startsWith("global{")) {
                    String content = line.substring("global{".length(), line.length() - 1);
                    String[] pairs = content.split(",");
                    for (String pair : pairs) {
                        String[] kv = pair.split(":");
                        if (kv.length == 2) {
                            String key = kv[0].trim();
                            String value = kv[1].trim();
                            globalConfig.put(key, value);
                            
                            if (key.equals("lossy_only")) {
                                String[] algos = value.split(";");
                                for (String algo : algos) {
                                    lossyOnlyAlgorithms.add(algo.trim());
                                }
                            }
                        } else if (kv.length == 1) {
                            String key = kv[0].trim();
                            AlgorithmEnums alg = AlgorithmEnums.CheckName(key);
                            if (alg != null) {
                                lossyOnlyAlgorithms.add(key);
                            }
                        }
                    }
                } else {
                    for (AlgorithmEnums alg : AlgorithmEnums.values()) {
                        if (line.startsWith(alg.getName() + "{")) {
                            String content = line.substring(alg.getName().length() + 1, line.length() - 1);
                            algorithmConfigs.put(alg.getName(), content);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Use default values if config file not found
        }
    }
}
