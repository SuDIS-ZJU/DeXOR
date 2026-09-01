package algorithms.Snappy;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.Snappy.decoder.DoubleSnappyDecoder;
import algorithms.Snappy.encoder.DoubleSnappyEncoder;
import enums.DataTypeEnums;

public class Snappy extends Algorithm {
    public Snappy(){
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleSnappyEncoder.class);
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleSnappyDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}