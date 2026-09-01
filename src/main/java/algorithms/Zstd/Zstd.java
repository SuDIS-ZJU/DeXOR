package algorithms.Zstd;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.Zstd.decoder.DoubleZstdDecoder;
import algorithms.Zstd.encoder.DoubleZstdEncoder;
import enums.DataTypeEnums;

public class Zstd extends Algorithm {
    public Zstd(){
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleZstdEncoder.class);
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleZstdDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}