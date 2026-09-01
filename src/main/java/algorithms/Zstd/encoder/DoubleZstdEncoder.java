package algorithms.Zstd.encoder;

import algorithms.Encoder;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DoubleZstdEncoder extends Encoder {
    private static final int BUFFER_SIZE = 4096;
    private final double[] buffer;
    private int bufferPosition = 0;
    private long totalValues = 0;
    private int compressionLevel = 3;

    public DoubleZstdEncoder(String outputPath) {
        this(outputPath, "");
    }
    
    public DoubleZstdEncoder(String outputPath, String config) {
        super(outputPath);
        
        Map<String, String> configMap = parseConfig(config);
        if (configMap.containsKey("level")) {
            try {
                compressionLevel = Integer.parseInt(configMap.get("level"));
            } catch (NumberFormatException e) {
                compressionLevel = 3;
            }
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

            byte[] compressed = Zstd.compress(uncompressed, compressionLevel);

            out.write(bufferPosition, 32);
            out.write(compressed.length, 32);
            
            for (int i = 0; i < compressed.length; i++) {
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