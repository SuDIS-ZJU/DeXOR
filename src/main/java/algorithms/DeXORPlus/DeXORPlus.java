package algorithms.DeXORPlus;

import algorithms.Algorithm;
import algorithms.DeXORPlus.decoder.DoubleDeXORPlusDecoder;
import algorithms.DeXORPlus.encoder.DoubleDeXORPlusEncoder;
import algorithms.Decoder;
import algorithms.Encoder;
import enums.DataTypeEnums;

public class DeXORPlus extends Algorithm {
    public DeXORPlus(){
        // Encoder
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleDeXORPlusEncoder.class);
        // Decoder
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleDeXORPlusDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}
