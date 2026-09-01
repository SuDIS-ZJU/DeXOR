package algorithms.Snappy.encoder;

import algorithms.Encoder;
import org.xerial.snappy.Snappy;

import java.util.HashMap;
import java.util.Map;

public class DoubleSnappyEncoder extends Encoder {
    private static final int BUFFER_SIZE = 4096;
    private final double[] buffer;
    private int bufferPosition = 0;
    private long totalValues = 0;

    public DoubleSnappyEncoder(String outputPath) {
        this(outputPath, "");
    }
    
    public DoubleSnappyEncoder(String outputPath, String config) {
        super(outputPath);
        this.buffer = new double[BUFFER_SIZE];
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

            byte[] compressed = Snappy.compress(uncompressed);

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