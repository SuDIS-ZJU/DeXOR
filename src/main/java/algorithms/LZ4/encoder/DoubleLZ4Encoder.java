package algorithms.LZ4.encoder;

import algorithms.Encoder;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.util.HashMap;
import java.util.Map;

public class DoubleLZ4Encoder extends Encoder {
    private static final int BUFFER_SIZE = 4096;
    private final double[] buffer;
    private int bufferPosition = 0;
    private final LZ4Compressor compressor;
    private long totalValues = 0;

    public DoubleLZ4Encoder(String outputPath) {
        this(outputPath, "");
    }
    
    public DoubleLZ4Encoder(String outputPath, String config) {
        super(outputPath);
        int level = 1;
        if (config != null && !config.isEmpty()) {
            Map<String, String> configMap = parseConfig(config);
            if (configMap.containsKey("level")) {
                try {
                    level = Integer.parseInt(configMap.get("level"));
                } catch (NumberFormatException e) {
                    level = 1;
                }
            }
        }
        if (level >= 9) {
            this.compressor = LZ4Factory.fastestInstance().highCompressor(level);
        } else {
            this.compressor = LZ4Factory.fastestInstance().fastCompressor();
        }
        this.buffer = new double[BUFFER_SIZE];
    }
    
    private Map<String, String> parseConfig(String config) {
        Map<String, String> map = new HashMap<>();
        if (config == null || config.isEmpty()) {
            return map;
        }
        
        if (config.startsWith("{") && config.endsWith("}")) {
            config = config.substring(1, config.length() - 1);
        }
        
        String[] pairs = config.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    @Override
    public int encode(double value) {
        buffer[bufferPosition++] = value;
        totalValues++;

        if (bufferPosition >= BUFFER_SIZE) {
            return flushBuffer();
        }
        return 0;
    }

    private int flushBuffer() {
        if (bufferPosition == 0) {
            return 0;
        }

        try {
            byte[] uncompressed = new byte[bufferPosition * 8];
            for (int i = 0; i < bufferPosition; i++) {
                long bits = Double.doubleToLongBits(buffer[i]);
                for (int j = 0; j < 8; j++) {
                    uncompressed[i * 8 + 7 - j] = (byte) ((bits >> (j * 8)) & 0xFF);
                }
            }

            byte[] compressed = new byte[compressor.maxCompressedLength(uncompressed.length)];
            int compressedLength = compressor.compress(uncompressed, 0, uncompressed.length, compressed, 0);

            out.write(bufferPosition, 32);
            out.write(compressedLength, 32);
            
            for (int i = 0; i < compressedLength; i++) {
                out.write(compressed[i], 8);
            }

            bufferPosition = 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return out.track_bits();
    }

    @Override
    public void flush() {
        flushBuffer();
    }

    @Override
    public int close() {
        flushBuffer();
        
        meta.put("total", (double) totalValues);

        out.clear();
        return 0;
    }
}
