package algorithms;

import algorithms.ALP.ALP;
import algorithms.Axe.Axe;
import algorithms.Camel.Camel;
import algorithms.Chimp.Chimp;
import algorithms.Chimp128.Chimp128;
import algorithms.DeXOR.DeXOR;
import algorithms.DeXORPlus.DeXORPlus;
import algorithms.Elf.Elf;
import algorithms.ElfPlus.ElfPlus;
import algorithms.ElfStar.ElfStar;
import algorithms.Gorilla.Gorilla;
import algorithms.LZ4.LZ4;
import algorithms.MixPiece.MixPiece;
import algorithms.SElfStar.SElfStar;
import algorithms.SimPiece.SimPiece;
import algorithms.Snappy.Snappy;
import algorithms.SZ3.SZ3;
import algorithms.SZ3v2.SZ3v2;
import algorithms.Zstd.Zstd;
import enums.AlgorithmEnums;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于管理所有压缩算法
 * Designed to oversee all compression algorithms,
 * this class serves as a centralized manager for various compression techniques.
 */

public class AlgorithmsManager {
    private static final Map<String, Class<?>> AlgorithmClassMap = new HashMap<>();

    static {
        AlgorithmClassMap.put(AlgorithmEnums.GORILLA.getName(), Gorilla.class);
        AlgorithmClassMap.put(AlgorithmEnums.CHIMP.getName(), Chimp.class);
        AlgorithmClassMap.put(AlgorithmEnums.CHIMP128.getName(), Chimp128.class);
        AlgorithmClassMap.put(AlgorithmEnums.DeXOR.getName(), DeXOR.class);
        AlgorithmClassMap.put(AlgorithmEnums.DeXORPlus.getName(), DeXORPlus.class);
        AlgorithmClassMap.put(AlgorithmEnums.Elf.getName(), Elf.class);
        AlgorithmClassMap.put(AlgorithmEnums.ElfPlus.getName(), ElfPlus.class);
        AlgorithmClassMap.put(AlgorithmEnums.Camel.getName(), Camel.class);
        AlgorithmClassMap.put(AlgorithmEnums.ALP.getName(), ALP.class);
        AlgorithmClassMap.put(AlgorithmEnums.ElfStar.getName(), ElfStar.class);
        AlgorithmClassMap.put(AlgorithmEnums.SElfStar.getName(), SElfStar.class);
        AlgorithmClassMap.put(AlgorithmEnums.LZ4.getName(), LZ4.class);
        AlgorithmClassMap.put(AlgorithmEnums.ZSTD.getName(), Zstd.class);
        AlgorithmClassMap.put(AlgorithmEnums.SNAPPY.getName(), Snappy.class);
        AlgorithmClassMap.put(AlgorithmEnums.SIMPIECE.getName(), SimPiece.class);
        AlgorithmClassMap.put(AlgorithmEnums.MIXPIECE.getName(), MixPiece.class);
        AlgorithmClassMap.put(AlgorithmEnums.SZ3.getName(), SZ3.class);
        AlgorithmClassMap.put(AlgorithmEnums.SZ3V2.getName(), SZ3v2.class);
        AlgorithmClassMap.put(AlgorithmEnums.AXE.getName(), Axe.class);
    }

    // todo Check_Valid
//    public static Set<String> getSupportedAlgorithms() {
//        return AlgorithmClassMap.keySet();
//    }

    public static Algorithm getAlgorithm(String algorithm_name) throws Exception {
        Class<?> clazz = AlgorithmClassMap.get(algorithm_name);
        if (clazz != null) {
            return (Algorithm) clazz.getDeclaredConstructor().newInstance();
        }
        throw new Exception("No Such Algorithm");
    }

    public static Encoder getEncoder(String data_type, String algorithm_name, String output_path) throws Exception {
        Algorithm instance = getAlgorithm(algorithm_name);
        return instance.getEncoder(data_type, output_path);
    }

    public static Decoder getDecoder(String data_type, String algorithm_name, String input_path) throws Exception {
        Algorithm instance = getAlgorithm(algorithm_name);
        return instance.getDecoder(data_type, input_path);
    }

    public static Encoder getEncoder(String data_type, String algorithm_name, String output_path, String config) throws Exception {
        Algorithm instance = getAlgorithm(algorithm_name);
        return instance.getEncoder(data_type, output_path, config);
    }

    public static Decoder getDecoder(String data_type, String algorithm_name, String input_path, String config) throws Exception {
        Algorithm instance = getAlgorithm(algorithm_name);
        return instance.getDecoder(data_type, input_path, config);
    }

}
