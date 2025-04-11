package algorithms;

import utils.StreamReader;

/**
 *  压缩/编码的公共抽象父类
 *  The common abstract superclass for compression.
 * */

public abstract class Decoder {
    protected StreamReader in;

    public Decoder(String inputPath) {
        this.in = new StreamReader(inputPath);
    }

    public int decodeInt() {
        return 0;
    }

    public long decodeLong() {
        return 0;
    }

    public float decodeFloat() {
        return 0;
    }

    public double decodeDouble() {
        return 0;
    }
}
