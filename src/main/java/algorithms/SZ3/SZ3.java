package algorithms.SZ3;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.SZ3.decoder.DoubleSZ3Decoder;
import algorithms.SZ3.encoder.DoubleSZ3Encoder;
import enums.DataTypeEnums;

public class SZ3 extends Algorithm {
    public SZ3() {
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleSZ3Encoder.class);
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleSZ3Decoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}