package algorithms.SZ3v2;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.SZ3v2.decoder.DoubleSZ3v2Decoder;
import algorithms.SZ3v2.encoder.DoubleSZ3v2Encoder;
import enums.DataTypeEnums;

public class SZ3v2 extends Algorithm {
    public SZ3v2() {
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleSZ3v2Encoder.class);
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleSZ3v2Decoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}