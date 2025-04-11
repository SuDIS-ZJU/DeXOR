package algorithms;

import utils.StreamWriter;

import java.util.HashMap;
import java.util.Map;

/**
 *  解压缩/解码的公共抽象父类
 *  The common abstract superclass for decompression.
 * */

public abstract class Encoder {

    protected StreamWriter out;
    protected Map<String, Double> meta = new HashMap<>();

    public Encoder(String outputPath) {
        this.out = new StreamWriter(outputPath);
    }

    public void flush() {
        this.out.clear();
    }

    public int encode(int value) {
        return 0;
    }

    public int encode(long value) {
        return 0;
    }

    public int encode(float value) {
        return 0;
    }

    public int encode(double value) {
        return 0;
    }


    public Map<String, Double> getMeta() {
        return meta;
    }
}
